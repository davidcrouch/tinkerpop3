package com.tinkerpop.gremlin.structure.strategy;


import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedVertex;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.*;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public interface VertexStrategy extends ElementStrategy, Vertex, Vertex.Iterators, StrategyWrapped, WrappedVertex<Vertex> {


    public default Vertex getBaseVertex() {
        if (getInnerVertex() instanceof StrategyWrapped)
            return ((VertexStrategy) getInnerVertex()).getBaseVertex();
        else
            return getInnerVertex();
    }

    public default Vertex getInnerVertex() {
        return (Vertex)getInnerElement();
    }

    /**
     * Add an outgoing edge to the vertex with provided label and edge properties as key/value pairs.
     * These key/values must be provided in an even number where the odd numbered arguments are {@link String}
     * property keys and the even numbered arguments are the related property values.
     *
     * @param label     The label of the edge
     * @param inVertex  The vertex to receive an incoming edge from the current vertex
     * @param keyValues The key/value pairs to turn into edge properties
     * @return the newly created edge
     */
    public default Edge addEdge(final String label, final Vertex inVertex, final Object... keyValues) {
        return graph().getStrategy().createEdgeStrategy(getInnerVertex().addEdge(label, inVertex, keyValues), graph());
    }


    @Override
    public default <V> VertexProperty<V> property(final String key, final V value) {
        return graph().getStrategy().createVertexPropertyStrategy(getInnerVertex().property(key, value), graph());

    }


    /**
     * Get the {@link Vertex.Iterators} implementation associated with this {@code Vertex}.
     * <p>
     * {@inheritDoc}
     */
    public default Vertex.Iterators iterators() {
        return this;
    }

    /**
     * Gets an {@link Iterator} of incident edges.
     *
     * @param direction  The incident direction of the edges to retrieve off this vertex
     * @param edgeLabels The labels of the edges to retrieve. If no labels are provided, then get all edges.
     * @return An iterator of edges meeting the provided specification
     */
    public default Iterator<Edge> edgeIterator(final Direction direction, final String... edgeLabels) {
        return StreamFactory.stream(getInnerVertex().iterators().edgeIterator(direction, edgeLabels)).map(edge -> graph().getStrategy().createEdgeStrategy(edge, graph())).iterator();
    }

    /**
     * Gets an {@link Iterator} of adjacent vertices.
     *
     * @param direction  The adjacency direction of the vertices to retrieve off this vertex
     * @param edgeLabels The labels of the edges associated with the vertices to retrieve. If no labels are provided, then get all edges.
     * @return An iterator of vertices meeting the provided specification
     */
    public default Iterator<Vertex> vertexIterator(final Direction direction, final String... edgeLabels) {
        return StreamFactory.stream(getInnerVertex().iterators().vertexIterator(direction, edgeLabels)).map(vertex -> graph().getStrategy().createVertexStrategy(vertex, graph())).iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public default <V> Iterator<VertexProperty<V>> propertyIterator(final String... propertyKeys) {
        return  StreamFactory.stream(getInnerVertex().iterators().<V>propertyIterator(propertyKeys)).map(property -> graph().getStrategy().createVertexPropertyStrategy(property, graph())).iterator();
    }


}
