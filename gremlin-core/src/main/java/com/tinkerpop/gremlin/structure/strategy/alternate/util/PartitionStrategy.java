package com.tinkerpop.gremlin.structure.strategy.alternate.util;

import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.graph.step.filter.HasStep;
import com.tinkerpop.gremlin.process.graph.util.DefaultGraphTraversal;
import com.tinkerpop.gremlin.process.graph.util.HasContainer;
import com.tinkerpop.gremlin.process.traverser.util.DefaultTraverserGeneratorFactory;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.StrategyWrapped;
import com.tinkerpop.gremlin.structure.strategy.alternate.*;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.util.StreamFactory;
import com.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;
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
public class PartitionStrategy extends DefaultStrategy implements Strategy {

    private PartitionGraph graph;

    private String writePartition;
    private final String partitionKey;
    private final Set<String> readPartitions = new HashSet<>();

    private PartitionStrategy(final String partitionKey, final String partition) {
        this.writePartition = partition;
        this.addReadPartition(partition);
        this.partitionKey = partitionKey;
    }


    public GraphStrategy graph() {
        return graph;
    }

    public GraphStrategy createGraphStrategy(Graph innerGraph) {
        this.graph = new PartitionGraph(innerGraph, this);
        return graph;
    }

    public PartitionVertex createVertexStrategy(Vertex innerVertex, GraphStrategy graph) {
        return new PartitionVertex(innerVertex, graph);
    }

    public PartitionEdge createEdgeStrategy(Edge innerEdge, GraphStrategy graph) {
        return new PartitionEdge(innerEdge, graph);
    }

    public <V> PropertyStrategy<V> createPropertyStrategy(Property<V> innerProperty, GraphStrategy graph) {
        return super.<V>createPropertyStrategy(innerProperty, graph);
    }

    public <V> VertexPropertyStrategy<V> createVertexPropertyStrategy(VertexProperty<V> innerVertexProperty, GraphStrategy graph) {
        return super.<V>createVertexPropertyStrategy(innerVertexProperty, graph);
    }

    //
    // Partition Strategy methods
    //

    public String getWritePartition() {
        return this.writePartition;
    }

    public void setWritePartition(final String writePartition) {
        this.writePartition = writePartition;
    }

    public String getPartitionKey() {
        return this.partitionKey;
    }

    public Set<String> getReadPartitions() {
        return Collections.unmodifiableSet(this.readPartitions);
    }

    public void removeReadPartition(final String readPartition) {
        this.readPartitions.remove(readPartition);
    }

    public void addReadPartition(final String readPartition) {
        this.readPartitions.add(readPartition);
    }

    public void clearReadPartitions() {
        this.readPartitions.clear();
    }


    //
    //  GRAPH
    //

    public final class PartitionGraph implements GraphStrategy {

        private Graph innerGraph;
        private PartitionStrategy strategy;

        public PartitionGraph(final Graph innerGraph, PartitionStrategy strategy) {
            this.innerGraph = innerGraph;
            this.strategy = strategy;
        }

        public Graph getInnerGraph() {
            return this.innerGraph;
        }

        public PartitionStrategy getStrategy() {
            return strategy;
        }

        @Override
        public PartitionVertex addVertex(final Object... keyValues) {
            return createVertexStrategy(getInnerGraph().addVertex(addKeyValues(keyValues)), this);
        }

        @Override
        public GraphTraversal<Vertex, Vertex> V(final Object... vertexIds) {

            final GraphTraversal<Vertex, Vertex> traversal = this.generateTraversal(getInnerGraph().getClass());
            traversal.asAdmin().getStrategies().setTraverserGeneratorFactory(DefaultTraverserGeneratorFactory.instance());
            TraversalHelper.insertTraversal(0, getInnerGraph().V(vertexIds).has(getPartitionKey(), Contains.within, getReadPartitions()), traversal);

            return traversal.filter(vertex -> testVertex(vertex.get()));
        }

        @Override
        public GraphTraversal<Edge, Edge> E(final Object... edgeIds) {

            final GraphTraversal<Edge, Edge> traversal = this.generateTraversal(getInnerGraph().getClass());
            traversal.asAdmin().getStrategies().setTraverserGeneratorFactory(DefaultTraverserGeneratorFactory.instance());
            TraversalHelper.insertTraversal(0, getInnerGraph().E(edgeIds).has(getPartitionKey(), Contains.within, getReadPartitions()), traversal);

            return traversal.filter(edge -> testEdge(edge.get()));
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
                    return super.toE(direction, edgeLabels).<Edge>has(getPartitionKey(), Contains.within, getReadPartitions()).filter(edge -> testEdge(edge.get()));
                }

                @Override
                public GraphTraversal<S, Vertex> toV(final Direction direction) {
                    return super.toV(direction).<Vertex>has(getPartitionKey(), Contains.within, getReadPartitions());
                }

                @Override
                public GraphTraversal<S, Vertex> otherV() {
                    return super.otherV().<Vertex>has(getPartitionKey(), Contains.within, getReadPartitions());
                }

                @Override
                public <E2 extends Element> GraphTraversal<S, E2> has(final String key, final BiPredicate predicate, final Object value) {
                    final HasContainer hasContainer = new HasContainer(key, predicate, value);
                    final HasStep<E2> hasStep = new HasStep<>(this, hasContainer);
                    hasStep.setPredicate(element -> hasContainer.test(((PartitionElement) element.get()).getBaseElement()));

                    return this.asAdmin().addStep(hasStep);
                }
            };
        }

    }

    //
    //  EDGE
    //

    public class PartitionEdge extends PartitionElement implements EdgeStrategy {

        public PartitionEdge(final Element innerElement, GraphStrategy graph) {
            super(innerElement, graph);
        }


        @Override
        public Iterator<Vertex> vertexIterator(final Direction direction) {
            return StreamFactory.stream(getInnerEdge().iterators().vertexIterator(direction)).filter(graph().<PartitionStrategy>getStrategy()::testVertex).map(vertex -> (Vertex) createVertexStrategy(vertex, graph())).iterator();
        }

        @Override
        public <V> Property<V> property(final String key) {
            return createPropertyStrategy(key.equals(getPartitionKey()) ? Property.<V>empty() : getInnerEdge().property(key), graph());
        }

        @Override
        public <V> Iterator<Property<V>> propertyIterator(final String... propertyKeys) {
            return StreamFactory.stream(getInnerEdge().iterators().<V>propertyIterator(propertyKeys)).filter(property -> !getPartitionKey().equals(property.key())).map(property -> (Property<V>) createPropertyStrategy(property, graph())).iterator();
        }


        @Override
        public Set<String> keys() {
            return IteratorUtils.fill(IteratorUtils.filter(getInnerEdge().keys().iterator(), key -> !getPartitionKey().equals(key)), new HashSet<>());
        }

    }

    //
    //  ELEMENT
    //

    public class PartitionElement implements ElementStrategy {

        private Element innerElement;
        private GraphStrategy graph;

        public PartitionElement(final Element innerElement, GraphStrategy graph) {
            this.innerElement = innerElement;
            this.graph = graph;
        }

        public Element getInnerElement() {
            return innerElement;
        }

        public GraphStrategy graph() {
            return this.graph;
        }

    }

    //
    //  VERTEX
    //

    public class PartitionVertex extends PartitionElement implements VertexStrategy {


        public PartitionVertex(final Vertex innerVertex, GraphStrategy graph) {
            super(innerVertex, graph);
        }

        @Override
        public PartitionEdge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
            return createEdgeStrategy(getInnerVertex().addEdge(label, inVertex, addKeyValues(keyValues)), graph());
        }

        @Override
        public Iterator<Vertex> vertexIterator(final Direction direction, final String... edgeLabels) {

            return StreamFactory
                    .stream(this.getInnerVertex().iterators().edgeIterator(direction, edgeLabels))
                    .filter(graph().<PartitionStrategy>getStrategy()::testEdge)
                    .map(edge -> otherVertex(direction, this, edge)).iterator();
        }

        public Iterator<Edge> edgeIterator(final Direction direction, final String... edgeLabels) {
            return IteratorUtils.filter(getInnerVertex().iterators().edgeIterator(direction, edgeLabels), graph().<PartitionStrategy>getStrategy()::testEdge);
        }

        @Override
        public <V> Iterator<VertexProperty<V>> propertyIterator(final String... propertyKeys) {
            return IteratorUtils.filter(getInnerVertex().iterators().propertyIterator(propertyKeys), property -> !(getPartitionKey().equals(property.key())));
        }

    }


    //
    //  Private methods
    //

    protected boolean testElement(final Element element) {
        final Property<String> property = element instanceof PartitionElement ?
                ((PartitionElement) element).getBaseElement().property(getPartitionKey()) :
                element.property(getPartitionKey());
        return property.isPresent() && getReadPartitions().contains(property.value());
    }

    protected boolean testVertex(final Vertex vertex) {
        return testElement(vertex);
    }

    protected boolean testEdge(final Edge edge) {
        // the edge must pass the edge predicate, and both of its incident vertices must also pass the vertex predicate
        // inV() and/or outV() will be empty if they do not.  it is sometimes the case that an edge is unwrapped
        // in which case it may not be filtered.  in such cases, the vertices on such edges should be tested.
        return testElement(edge)
                && (edge instanceof StrategyWrapped ? edge.iterators().vertexIterator(Direction.IN).hasNext() && edge.iterators().vertexIterator(Direction.OUT).hasNext()
                : testVertex(edge.iterators().vertexIterator(Direction.IN).next()) && testVertex(edge.iterators().vertexIterator(Direction.OUT).next()));
    }

    protected static final Vertex otherVertex(final Direction direction, final Vertex start, final Edge edge) {
        if (direction.equals(Direction.BOTH)) {
            final Vertex inVertex = edge.iterators().vertexIterator(Direction.IN).next();
            return ElementHelper.areEqual(start, inVertex) ?
                    edge.iterators().vertexIterator(Direction.OUT).next() :
                    inVertex;
        } else {
            return edge.iterators().vertexIterator(direction.opposite()).next();
        }
    }


    protected final Object[] addKeyValues(final Object[] keyValues) {
        final Object[] keyValuesExtended = Arrays.copyOf(keyValues, keyValues.length + 2);
        keyValuesExtended[keyValues.length] = getPartitionKey();
        keyValuesExtended[keyValues.length + 1] = getWritePartition();
        return keyValuesExtended;
    }

    //
    //  Builder
    //

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private String startPartition = "default";
        private String partitionKey = "_partition";

        private Builder() {
        }

        /**
         * The initial partition to filter by. If this value is not set, it will be defaulted to "default".
         */
        public Builder startPartition(final String startPartition) {
            if (null == startPartition) throw new IllegalArgumentException("The startPartition cannot be null");
            this.startPartition = startPartition;
            return this;
        }

        /**
         * The name of the partition key.  If this is not set, then the value is defaulted to "_partition".
         */
        public Builder partitionKey(final String partitionKey) {
            if (null == partitionKey) throw new IllegalArgumentException("The partitionKey cannot be null");
            this.partitionKey = partitionKey;
            return this;
        }

        public PartitionStrategy create() {
            return new PartitionStrategy(partitionKey, startPartition);
        }
    }
}