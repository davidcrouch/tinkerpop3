package com.tinkerpop.gremlin.structure.strategy.util;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.*;

import java.util.Iterator;

/**
 * Created by davidcrouch on 1/27/2015.
 */
public interface DefaultStrategy extends Strategy {

    public default Graph createGraphStrategy(Graph innerGraph) {
        return new DefaultGraphStrategy(innerGraph, this);
    }

    public default Vertex createVertexStrategy(Vertex innerVertex, GraphStrategy graph) {
        return new DefaultVertexStrategy(innerVertex, graph);
    }

    public default Edge createEdgeStrategy(Edge innerEdge, GraphStrategy graph) {
        return new DefaultEdgeStrategy(innerEdge, graph);
    }

    public default <V> Property<V> createPropertyStrategy(Property<V> innerProperty, GraphStrategy graph) {
        return new DefaultPropertyStrategy<V>(innerProperty, graph);
    }

    public default <V> VertexProperty<V> createVertexPropertyStrategy(VertexProperty<V> innerVertexProperty, GraphStrategy graph) {
        return new DefaultVertexPropertyStrategy<>(innerVertexProperty, graph);
    }
}
