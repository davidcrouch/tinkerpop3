package com.tinkerpop.gremlin.server.op.standard;

import com.codahale.metrics.Timer;
import com.tinkerpop.gremlin.server.Context;
import com.tinkerpop.gremlin.server.GremlinExecutor;
import com.tinkerpop.gremlin.server.GremlinServer;
import com.tinkerpop.gremlin.server.MessageSerializer;
import com.tinkerpop.gremlin.server.RequestMessage;
import com.tinkerpop.gremlin.server.ResultCode;
import com.tinkerpop.gremlin.server.ScriptEngines;
import com.tinkerpop.gremlin.server.Settings;
import com.tinkerpop.gremlin.server.Tokens;
import com.tinkerpop.gremlin.server.op.OpProcessorException;
import com.tinkerpop.gremlin.server.util.LocalExecutorService;
import com.tinkerpop.gremlin.server.util.MetricManager;
import com.tinkerpop.gremlin.process.util.SingleIterator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Operations to be used by the {@link StandardOpProcessor}.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
final class StandardOps {
    private static final Logger logger = LoggerFactory.getLogger(StandardOps.class);
    private static final Timer evalOpTimer = MetricManager.INSTANCE.getTimer(name(GremlinServer.class, "op", "eval"));

    /**
     * Modify the imports on the {@code ScriptEngine}.
     */
    public static void importOp(final Context context) {
        final RequestMessage msg = context.getRequestMessage();
        final List<String> l = (List<String>) msg.args.get(Tokens.ARGS_IMPORTS);
        context.getGremlinExecutor().getSharedScriptEngines().addImports(new HashSet<>(l));
    }

    /**
     * List the dependencies, imports, or variables in the {@code ScriptEngine}.
     */
    public static void showOp(final Context context) {
        final RequestMessage msg = context.getRequestMessage();
        final ChannelHandlerContext ctx = context.getChannelHandlerContext();
        final MessageSerializer serializer = MessageSerializer.select(
                msg.<String>optionalArgs(Tokens.ARGS_ACCEPT).orElse("text/plain"),
                MessageSerializer.DEFAULT_RESULT_SERIALIZER);
        final String infoType = msg.<String>optionalArgs(Tokens.ARGS_INFO_TYPE).get();
        final GremlinExecutor executor = context.getGremlinExecutor();
        final ScriptEngines scriptEngines = executor.getSharedScriptEngines();

        final Object infoToShow;
        if (infoType.equals(Tokens.ARGS_INFO_TYPE_DEPDENENCIES))
            infoToShow = scriptEngines.dependencies();
        else if (infoType.equals(Tokens.ARGS_INFO_TYPE_IMPORTS))
            infoToShow = scriptEngines.imports();
        else {
            // this shouldn't happen if validations are working properly.  will bomb and log as error to server logs
            // thus killing the connection
            throw new RuntimeException(String.format("Validation for the show operation is not properly checking the %s", Tokens.ARGS_INFO_TYPE));
        }

        try {
            ctx.channel().write(new TextWebSocketFrame(serializer.serializeResult(infoToShow, context)));
        } catch (Exception ex) {
            logger.warn("The result [{}] in the request {} could not be serialized and returned.",
                    infoToShow, context.getRequestMessage(), ex);
        }
    }

    /**
     * Resets the {@code ScriptEngine} thus forcing a reload of scripts and classes.
     */
    public static void resetOp(final Context context) {
        context.getGremlinExecutor().getSharedScriptEngines().reset();
    }

    /**
     * Pull in maven based dependencies and load Gremlin plugins.
     */
    public static void useOp(final Context context) {
        final RequestMessage msg = context.getRequestMessage();
        final MessageSerializer serializer = MessageSerializer.select(
                msg.<String>optionalArgs(Tokens.ARGS_ACCEPT).orElse("text/plain"),
                MessageSerializer.DEFAULT_RESULT_SERIALIZER);
        final List<Map<String, String>> usings = (List<Map<String, String>>) msg.args.get(Tokens.ARGS_COORDINATES);
        usings.forEach(c -> {
            final String group = c.get(Tokens.ARGS_COORDINATES_GROUP);
            final String artifact = c.get(Tokens.ARGS_COORDINATES_ARTIFACT);
            final String version = c.get(Tokens.ARGS_COORDINATES_VERSION);
            logger.info("Loading plugin [group={},artifact={},version={}]", group, artifact, version);
            context.getGremlinExecutor().getSharedScriptEngines().use(group, artifact, version);

            final Map<String, String> coords = new HashMap<String, String>() {{
                put("group", group);
                put("artifact", artifact);
                put("version", version);
            }};

            context.getChannelHandlerContext().channel().write(new TextWebSocketFrame(serializer.serializeResult(coords, context)));
        });
    }

    /**
     * Evaluate a script in the {@code ScriptEngine}.
     */
    public static void evalOp(final Context context) throws OpProcessorException {
        final Timer.Context timerContext = evalOpTimer.time();
        final ChannelHandlerContext ctx = context.getChannelHandlerContext();
        final RequestMessage msg = context.getRequestMessage();
        final Settings settings = context.getSettings();
        final MessageSerializer serializer = MessageSerializer.select(
                msg.<String>optionalArgs(Tokens.ARGS_ACCEPT).orElse("text/plain"),
                MessageSerializer.DEFAULT_RESULT_SERIALIZER);

        // a worker service bound to the current thread
        final ExecutorService executorService = LocalExecutorService.getLocal();

        // a marker to determine if the code has succeeded at evaluation of the script
        boolean evaluated = false;

        // the task that is doing the serialization work
        Optional<Future<Void>> serializing = Optional.empty();

        // timer for the total serialization time
        final StopWatch stopWatch = new StopWatch();

        // the result from the script evaluation
        Object o;
        try {
            // evaluate the script
            o = context.getGremlinExecutor().eval(msg, context.getGraphs());

            // make all results an iterator
            final Iterator itty = convertToIterator(o);

            // mark this request as having its results evaluated
            evaluated = true;
            stopWatch.start();

            // determines when the iteration of all results is complete and queue is full
            final AtomicBoolean iterationComplete = new AtomicBoolean(false);

            // need to queue results as the frames need to be written in the request that handled the response.
            // hard to write a good integration test for the serialization/response timeouts.  add a Thread.sleep()
            // into the Callable to force different scenarios or to throw exceptions.
            final ArrayBlockingQueue<TextWebSocketFrame> frameQueue = new ArrayBlockingQueue<>(settings.frameQueueSize);
            serializing = Optional.of(executorService.submit((Callable<Void>) () -> {
                while (itty.hasNext()) {
                    final Object individualResult = itty.next();
                    try {
                        if (logger.isDebugEnabled())
                            logger.debug("Writing to frame queue [{}] for msg [{}]", individualResult, msg);

                        frameQueue.put(new TextWebSocketFrame(true, 0, serializer.serializeResult(individualResult, context)));
                    } catch (InterruptedException ie) {
                        // InterruptedException occurs here if the the serialization of an individual element exceeds
                        // the configured time for serializeResultTimeout.  the loop will exit here and a failure
                        // message was already written by a separate process to the client.
                        logger.warn("Serialization cancelled as the total request size was exceeded.", ie);
                    } catch (Exception ex) {
                        // this is generally just serialization exceptions. write back the error then exit the
                        // serialization loop
                        logger.warn("The result [{}] in the request {} could not be serialized and returned.", individualResult, context.getRequestMessage(), ex);
                        final String errorMessage = String.format("Error during serialization: %s",
                                ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                        frameQueue.put(new TextWebSocketFrame(serializer.serializeResult(errorMessage, ResultCode.SERVER_ERROR_SERIALIZATION, context)));
                        break;
                    }
                }

                iterationComplete.set(true);
                return null;
            }));

            do {
                // poll for frames placed in the queue by the process in the executor service.  the poll is governed by
                // the serializeResultTimeout and if it returns null and the iteration of all results is not yet
                // finished then that means that the serialization of a single element is just taking too long and
                // the timeout is exceeded so an exception is thrown.  the loop can get into state where a poll for
                // the specified serializeResultTimeout will return null and iteration will be complete.  In such
                // cases the logger is the only one that needs to know about it.
                final Optional<TextWebSocketFrame> currentFrame
                        = Optional.ofNullable(frameQueue.poll(settings.serializeResultTimeout, TimeUnit.MILLISECONDS));
                if (currentFrame.isPresent()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Writing from frame queue to client [{}] for msg [{}]", currentFrame.get(), msg);

                    ctx.channel().write(currentFrame.get());
                } else {
                    if (!iterationComplete.get())
                        throw new TimeoutException("Serialization of an individual result exceeded the serializeResultTimeout setting");
                    else
                        logger.debug("The Frame queue was empty and iteration is complete.");
                }

                stopWatch.split();
                if (stopWatch.getSplitTime() > settings.serializedResponseTimeout)
                    throw new TimeoutException("Serialization of the entire response exceeded the serializeResponseTimeout setting");

                stopWatch.unsplit();
            } while (!iterationComplete.get() || frameQueue.size() > 0);

        } catch (ScriptException se) {
            logger.debug("Exception from ScriptException error.", se);
            throw new OpProcessorException(String.format("Error while evaluating a script on request [%s]", msg),
                    serializer.serializeResult(se.getMessage(), ResultCode.SERVER_ERROR_SCRIPT_EVALUATION, context));
        } catch (InterruptedException ie) {
            logger.debug("Exception from InterruptedException error.", ie);
            throw new OpProcessorException(String.format("Thread interrupted (perhaps script ran for too long) while processing this request [%s]", msg),
                    serializer.serializeResult(ie.getMessage(), ResultCode.SERVER_ERROR, context));
        } catch (ExecutionException ee) {
            logger.debug("Exception from ExecutionException error.", ee.getCause());
            Throwable inner = ee.getCause();
            if (inner instanceof ScriptException)
                inner = inner.getCause();

            throw new OpProcessorException(String.format("Error while processing response from the script evaluated on request [%s]", msg),
                    serializer.serializeResult(inner.getMessage(), ResultCode.SERVER_ERROR, context));
        } catch (TimeoutException toe) {
            final String errorMessage;
            if (!evaluated)
                errorMessage = String.format("Script evaluation exceeded the configured threshold of %s ms for request [%s]", settings.scriptEvaluationTimeout, msg);
            else
                errorMessage = String.format("Response iteration and serialization exceeded the configured threshold for request [%s] - %s", msg, toe.getMessage());

            throw new OpProcessorException(errorMessage, serializer.serializeResult(errorMessage, ResultCode.SERVER_ERROR_TIMEOUT, context));
        } finally {
            // try to cancel the serialization task if its still running somehow
            serializing.ifPresent(f -> f.cancel(true));

            // the stopwatch was only started after a script was evaluated
            if (evaluated)
                stopWatch.stop();

            timerContext.stop();
        }
    }

    private static Iterator convertToIterator(Object o) {
        Iterator itty;
        if (o instanceof Iterable)
            itty = ((Iterable) o).iterator();
        else if (o instanceof Iterator)
            itty = (Iterator) o;
        else if (o instanceof Object[])
            itty = new ArrayIterator(o);
        else if (o instanceof Stream)
            itty = ((Stream) o).iterator();
        else if (o instanceof Map)
            itty = ((Map) o).entrySet().iterator();
        else if (o instanceof Throwable)
            itty = new SingleIterator<Object>(((Throwable) o).getMessage());
        else
            itty = new SingleIterator<>(o);
        return itty;
    }
}