package com.tinkerpop.gremlin.structure.strategy.util.partition;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.strategy.VertexStrategy;
import com.tinkerpop.gremlin.util.StreamFactory;
import com.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.Iterator;

/**
 * Created by davidcrouch on 1/27/2015.
 */
public class PartitionVertex extends PartitionElement implements VertexStrategy {


    public PartitionVertex(final Vertex innerVertex, Graph graph) {
        super(innerVertex, graph);
    }

    @Override
    public Edge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
        return strategy.createEdgeStrategy(getInnerVertex().addEdge(label, inVertex, strategy.addKeyValues(keyValues)), graph());
    }

    @Override
    public Iterator<Vertex> vertexIterator(final Direction direction, final String... edgeLabels) {

        return StreamFactory
                .stream(this.getInnerVertex().iterators().edgeIterator(direction, edgeLabels))
                .filter(strategy::testEdge)
                .map(edge -> strategy.otherVertex(direction, this, edge)).iterator();
    }

    public Iterator<Edge> edgeIterator(final Direction direction, final String... edgeLabels) {
        return IteratorUtils.filter(getInnerVertex().iterators().edgeIterator(direction, edgeLabels), strategy::testEdge);
    }

    @Override
    public <V> Iterator<VertexProperty<V>> propertyIterator(final String... propertyKeys) {
        return IteratorUtils.filter(getInnerVertex().iterators().propertyIterator(propertyKeys), property -> !(graph().<PartitionStrategy>getStrategy().getPartitionKey().equals(property.key())));
    }

}
