package com.tinkerpop.gremlin.structure.strategy.util.partition;

import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.graph.step.filter.HasStep;
import com.tinkerpop.gremlin.process.graph.util.DefaultGraphTraversal;
import com.tinkerpop.gremlin.process.graph.util.HasContainer;
import com.tinkerpop.gremlin.process.traverser.util.DefaultTraverserGeneratorFactory;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.strategy.Strategy;
import com.tinkerpop.gremlin.structure.strategy.StrategyWrapped;
import com.tinkerpop.gremlin.structure.strategy.util.*;
import com.tinkerpop.gremlin.structure.util.ElementHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * A {@link com.tinkerpop.gremlin.structure.strategy.GraphStrategy} which enables support for logical graph partitioning where the Graph can be blinded to
 * different parts of the total {@link com.tinkerpop.gremlin.structure.Graph}.  Note that the {@code partitionKey}
 * is hidden by this strategy.  Use the base {@link com.tinkerpop.gremlin.structure.Graph} to access that.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Joshua Shinavier (http://fortytwo.net)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class PartitionGraph extends DefaultGraphStrategy implements GraphStrategy {

    public PartitionGraph(final Graph innerGraph, Strategy strategy) {
        super(innerGraph, strategy);

    }

    @Override
    public PartitionStrategy getStrategy() {
        return super.getStrategy();
    }

    @Override
    public Vertex addVertex(final Object... keyValues) {
        return getStrategy().createVertexStrategy(getInnerGraph().addVertex(getStrategy().addKeyValues(keyValues)), this);
    }

    @Override
    public GraphTraversal<Vertex, Vertex> V(final Object... vertexIds) {

        final GraphTraversal<Vertex, Vertex> traversal = this.generateTraversal(getInnerGraph().getClass());
        traversal.asAdmin().getStrategies().setTraverserGeneratorFactory(DefaultTraverserGeneratorFactory.instance());
        TraversalHelper.insertTraversal(0, getInnerGraph().V(vertexIds).has(getStrategy().getPartitionKey(), Contains.within, getStrategy().getReadPartitions()), traversal);
        return traversal.filter(vertex -> getStrategy().testVertex(vertex.get()));
    }

    @Override
    public GraphTraversal<Edge, Edge> E(final Object... edgeIds) {

        final GraphTraversal<Edge, Edge> traversal = this.generateTraversal(getInnerGraph().getClass());
        traversal.asAdmin().getStrategies().setTraverserGeneratorFactory(DefaultTraverserGeneratorFactory.instance());
        TraversalHelper.insertTraversal(0, getInnerGraph().E(edgeIds).has(getStrategy().getPartitionKey(), Contains.within, getStrategy().getReadPartitions()), traversal);
        return traversal.filter(edge -> getStrategy().testEdge(edge.get()));
    }


    private final <S, E> GraphTraversal<S, E> generateTraversal(final Class emanatingClass) {
        return new DefaultGraphTraversal<S, E>(emanatingClass) {
            @Override
            public GraphTraversal<S, Vertex> to(final Direction direction, final String... edgeLabels) {
                return direction.equals(Direction.BOTH) ?
                        this.toE(direction, edgeLabels).otherV() :
                        this.toE(direction, edgeLabels).toV(direction.opposite());
            }

            @Override
            public GraphTraversal<S, Edge> toE(final Direction direction, final String... edgeLabels) {
                return super.toE(direction, edgeLabels).<Edge>has(getStrategy().getPartitionKey(), Contains.within, getStrategy().getReadPartitions()).filter(edge -> getStrategy().testEdge(edge.get()));
            }

            @Override
            public GraphTraversal<S, Vertex> toV(final Direction direction) {
                return super.toV(direction).<Vertex>has(getStrategy().getPartitionKey(), Contains.within, getStrategy().getReadPartitions());
            }

            @Override
            public GraphTraversal<S, Vertex> otherV() {
                return super.otherV().<Vertex>has(getStrategy().getPartitionKey(), Contains.within, getStrategy().getReadPartitions());
            }

            @Override
            public <E2 extends Element> GraphTraversal<S, E2> has(final String key, final BiPredicate predicate, final Object value) {
                final HasContainer hasContainer = new HasContainer(key, predicate, value);
                final HasStep<E2> hasStep = new HasStep<>(this, hasContainer);
                hasStep.setPredicate(element -> hasContainer.test(((DefaultElementStrategy) element.get()).getBaseElement()));
                return this.asAdmin().addStep(hasStep);
            }
        };
    }


}