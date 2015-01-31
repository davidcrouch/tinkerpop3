package com.tinkerpop.gremlin.structure.strategy;

import com.tinkerpop.gremlin.structure.*;

import java.util.Iterator;

/**
 * Created by davidcrouch on 1/27/2015.
 */
public interface Strategy {

    public Graph createGraphStrategy(Graph innerGraph);
    public Vertex createVertexStrategy(Vertex innerVertex, Graph graph);
    public Edge  createEdgeStrategy(Edge innerEdge, Graph graph);
    public <V> VertexProperty<V> createVertexPropertyStrategy(VertexProperty<V> innerVertexProperty, Graph graph);
    public <V> Property<V> createPropertyStrategy(Property<V> innerProperty, Graph graph);

//    public Iterator<Vertex> createVertexIteratorStrategy(Iterator<Vertex> innerVertexIterator, GraphStrategy graph);
//    public Iterator<Edge> createEdgeIteratorStrategy(Iterator<Edge> innerEdgeIterator, GraphStrategy graph);
//    public <V> Iterator<Property<V>> createPropertyIteratorStrategy(Iterator<Property<V>> innerPropertyIterator, GraphStrategy graph);
//    public <V> Iterator<VertexProperty<V>> createVertexPropertyIteratorStrategy(Iterator<VertexProperty<V>> innerVertexPropertyIterator, GraphStrategy graph);

}
