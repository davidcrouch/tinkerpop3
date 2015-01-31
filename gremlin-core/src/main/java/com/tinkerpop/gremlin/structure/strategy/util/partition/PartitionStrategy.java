package com.tinkerpop.gremlin.structure.strategy.util.partition;

import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.graph.step.filter.HasStep;
import com.tinkerpop.gremlin.process.graph.util.DefaultGraphTraversal;
import com.tinkerpop.gremlin.process.graph.util.HasContainer;
import com.tinkerpop.gremlin.process.traverser.util.DefaultTraverserGeneratorFactory;
import com.tinkerpop.gremlin.process.util.TraversalHelper;
import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.*;
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
public class PartitionStrategy implements DefaultStrategy {

    private  Graph innerGraph;
    private String writePartition;
    private final String partitionKey;
    private final Set<String> readPartitions = new HashSet<>();

    private PartitionStrategy(final String partitionKey, final String partition) {
        this.writePartition = partition;
        this.addReadPartition(partition);
        this.partitionKey = partitionKey;
    }


    public Graph createGraphStrategy(Graph innerGraph) {
        return new PartitionGraph(innerGraph, this);
    }


    public Vertex createVertexStrategy(Vertex innerVertex, Graph graph) {
        return new PartitionVertex(innerVertex, graph);
    }

    public Edge createEdgeStrategy(Edge innerEdge, Graph graph) {
        return new PartitionEdge(innerEdge, graph);
    }

    public <V> Property<V> createPropertyStrategy(Property<V> innerProperty, Graph graph) {
        return new DefaultPropertyStrategy<V>(innerProperty, graph);
    }

    public <V> VertexProperty<V> createVertexPropertyStrategy(VertexProperty<V> innerVertexProperty, Graph graph) {
        return new DefaultVertexPropertyStrategy<>(innerVertexProperty, graph);
    }


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



    protected boolean testElement(final Element element) {
        final Property<String> property = element instanceof DefaultElementStrategy ?
                ((DefaultElementStrategy) element).getBaseElement().property(getPartitionKey()) :
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