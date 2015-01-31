package com.tinkerpop.gremlin.structure.strategy.util.partition;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.EdgeStrategy;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.strategy.util.DefaultEdgeStrategy;
import com.tinkerpop.gremlin.util.StreamFactory;
import com.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Created by davidcrouch on 1/27/2015.
 */
public class PartitionEdge extends PartitionElement implements EdgeStrategy {

    public PartitionEdge(final Element innerElement, Graph graph) {
        super(innerElement, graph);
    }


    @Override
    public Iterator<Vertex> vertexIterator(final Direction direction) {
        return StreamFactory.stream(getInnerEdge().iterators().vertexIterator(direction)).filter(strategy::testVertex).map(vertex -> (Vertex)graph().getStrategy().createVertexStrategy(vertex, graph())).iterator();
    }

    @Override
    public <V> Property<V> property(final String key) {
        return strategy.createPropertyStrategy(key.equals(strategy.getPartitionKey()) ? Property.<V>empty() : getInnerEdge().property(key), graph());
    }

    @Override
    public <V> Iterator<Property<V>> propertyIterator(final String... propertyKeys) {
        return StreamFactory.stream(getInnerEdge().iterators().<V>propertyIterator(propertyKeys)).filter(property -> !strategy.getPartitionKey().equals(property.key())).map(property -> (Property<V>)graph().getStrategy().createPropertyStrategy(property, graph())).iterator();
    }


    @Override
    public Set<String> keys() {
        return IteratorUtils.fill(IteratorUtils.filter(getInnerEdge().keys().iterator(), key -> !strategy.getPartitionKey().equals(key)), new HashSet<>());
    }

}
