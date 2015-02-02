package com.tinkerpop.gremlin.structure.strategy.alternate;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.StrategyWrapped;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedEdge;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.util.Iterator;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public interface EdgeStrategy extends ElementStrategy, Edge, Edge.Iterators, StrategyWrapped, WrappedEdge<Edge> {

    /**
     * Gets the underlying base {@link Edge} that is being hosted within this wrapper.
     */
    public default Edge getBaseEdge() {
        if (getInnerEdge() instanceof StrategyWrapped)
            return ((EdgeStrategy)getInnerEdge()).getBaseEdge();
        else
            return getInnerEdge();
    }

    public default Edge getInnerEdge() {
        return (Edge)getInnerElement();
    }


    public default Edge.Iterators iterators() {
        return this;
    }

    /**
     * Retrieve the vertex (or vertices) associated with this edge as defined by the direction.
     * If the direction is {@link Direction#BOTH} then the iterator order is: {@link Direction#OUT} then {@link Direction#IN}.
     *
     * @param direction Get the incoming vertex, outgoing vertex, or both vertices
     * @return An iterator with 1 or 2 vertices
     */
    public default Iterator<Vertex> vertexIterator(final Direction direction) {
        return StreamFactory.stream(getInnerEdge().iterators().vertexIterator(direction)).map(vertex -> (Vertex)graph().getStrategy().createVertexStrategy(vertex, graph())).iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public default <V> Iterator<Property<V>> propertyIterator(final String... propertyKeys) {
        return  StreamFactory.stream(getInnerElement().iterators().<V>propertyIterator(propertyKeys)).map(property -> (Property<V>)graph().getStrategy().createPropertyStrategy(property, graph())).iterator();
    }


}
