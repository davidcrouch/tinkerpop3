package com.tinkerpop.gremlin.structure.strategy.alternate;

import com.tinkerpop.gremlin.structure.*;

import java.util.Iterator;

/**
 * Created by davidcrouch on 1/27/2015.
 */
public interface Strategy {

    public GraphStrategy createGraphStrategy(Graph innerGraph);
    public VertexStrategy createVertexStrategy(Vertex innerVertex, GraphStrategy graph);
    public EdgeStrategy  createEdgeStrategy(Edge innerEdge, GraphStrategy graph);
    public <V> VertexPropertyStrategy<V> createVertexPropertyStrategy(VertexProperty<V> innerVertexProperty, GraphStrategy graph);
    public <V> PropertyStrategy<V> createPropertyStrategy(Property<V> innerProperty, GraphStrategy graph);

//    public Iterator<Vertex> createVertexIteratorStrategy(Iterator<Vertex> innerVertexIterator, GraphStrategy graph);
//    public Iterator<Edge> createEdgeIteratorStrategy(Iterator<Edge> innerEdgeIterator, GraphStrategy graph);
//    public <V> Iterator<Property<V>> createPropertyIteratorStrategy(Iterator<Property<V>> innerPropertyIterator, GraphStrategy graph);
//    public <V> Iterator<VertexProperty<V>> createVertexPropertyIteratorStrategy(Iterator<VertexProperty<V>> innerVertexPropertyIterator, GraphStrategy graph);

}
