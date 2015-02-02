package com.tinkerpop.gremlin.structure.strategy.alternate;


import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Transaction;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.strategy.StrategyWrapped;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedGraph;
import com.tinkerpop.gremlin.util.StreamFactory;
import org.apache.commons.configuration.Configuration;

import java.util.Iterator;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public interface GraphStrategy extends Graph, Graph.Iterators, StrategyWrapped, WrappedGraph<Graph> {

    /**
     * Gets the underlying base {@link Graph} that is being hosted within this wrapper.
     */
    public default Graph getBaseGraph() {
        if (getInnerGraph() instanceof StrategyWrapped)
            return ((GraphStrategy)getInnerGraph()).getBaseGraph();
        else
            return getInnerGraph();
    }

    /**
     * Gets the gets the adjacent inner {@link Graph} that this instance wraps.
     */
    public Graph getInnerGraph();


    /**
     * Add a {@link com.tinkerpop.gremlin.structure.Vertex} to the graph given an optional series of key/value pairs.  These key/values
     * must be provided in an even number where the odd numbered arguments are {@link String} property keys and the
     * even numbered arguments are the related property values.
     *
     * @param keyValues The key/value pairs to turn into vertex properties
     * @return The newly created vertex
     */
    public default Vertex addVertex(final Object... keyValues) {
        return this.getStrategy().createVertexStrategy(getInnerGraph().addVertex(keyValues), this);
    }


    /**
     * Starts a {@link com.tinkerpop.gremlin.process.graph.GraphTraversal} over the vertices in the graph.
     * If vertexIds are provided, then the traversal starts at those vertices, else all vertices in the graph.
     *
     * @param vertexIds the ids of the vertices to get (if none are provided, get all vertices)
     * @return a graph traversal over the vertices of the graph
     */
    public default GraphTraversal<Vertex, Vertex> V(final Object... vertexIds) {
        return getInnerGraph().V(vertexIds).map(vertex -> this.getStrategy().createVertexStrategy(vertex.get(), this));
    }

    /**
     * Starts a {@link GraphTraversal} over the edges in the graph.
     * If edgeIds are provided, then the traversal starts at those edges, else all edges in the graph.
     *
     * @param edgeIds the ids of the edges to get (if none are provided, get all edges)
     * @return a graph traversal over the edges of the graph
     */
    public default GraphTraversal<Edge, Edge> E(final Object... edgeIds) {
        return getInnerGraph().E(edgeIds).map(edge -> this.getStrategy().createEdgeStrategy(edge.get(), this));
    }


    /**
     * Create an OLAP {@link com.tinkerpop.gremlin.process.computer.GraphComputer} to execute a vertex program over this graph.
     * If the graph does not support graph computer then an {@link java.lang.UnsupportedOperationException} is thrown.
     * The provided arguments can be of either length 0 or 1. A graph can support multiple graph computers.
     *
     * @param graphComputerClass The graph computer class to use (if no argument, then a default is selected by the graph)
     * @return A graph computer for processing this graph
     */
    public default GraphComputer compute(final Class... graphComputerClass) {
        return getInnerGraph().compute(graphComputerClass);
    }

    /**
     * Configure and control the transactions for those graphs that support this feature.
     */
    public default Transaction tx() {
        return getInnerGraph().tx();
    }



    public <V extends Strategy> V getStrategy();

    /**
     * A collection of global {@link Graph.Variables} associated with the graph.
     * Variables are used for storing metadata about the graph.
     *
     * @return The variables associated with this graph
     */
    public default Graph.Variables variables() {
        return getInnerGraph().variables();
    }

    /**
     * Get the {@link org.apache.commons.configuration.Configuration} associated with the construction of this graph.
     * Whatever configuration was passed to {@link com.tinkerpop.gremlin.structure.util.GraphFactory#open(org.apache.commons.configuration.Configuration)}
     * is what should be returned by this method.
     *
     * @return the configuration used during graph construction.
     */
    public default Configuration configuration() {
        return getInnerGraph().configuration();
    }

    /**
     * Get the {@link Graph.Iterators} associated with this graph.
     *
     * @return the graph iterators of this graph
     */
    public default Graph.Iterators iterators() {
        return this;
    }

    /**
     * Gets the {@link Features} exposed by the underlying {@code Graph} implementation.
     */
    public default Features features() {
        return getInnerGraph().features();
    }


    public default Iterator<Vertex> vertexIterator(final Object... vertexIds) {
        return StreamFactory.stream(getInnerGraph().iterators().vertexIterator(vertexIds)).map(vertex -> (Vertex)this.getStrategy().createVertexStrategy(vertex, this)).iterator();
    }


    public default Iterator<Edge> edgeIterator(final Object... edgeIds) {
        return  StreamFactory.stream(getInnerGraph().iterators().edgeIterator(edgeIds)).map(edge -> (Edge)this.getStrategy().createEdgeStrategy(edge, this)).iterator();
    }

    public default void close() throws Exception {
        getInnerGraph().close();
    }

}
