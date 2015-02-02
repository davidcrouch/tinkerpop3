package com.tinkerpop.gremlin.structure.strategy.alternate.util;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.alternate.*;

/**
 * Created by davidcrouch on 1/27/2015.
 */
public class DefaultStrategy implements Strategy {

    public GraphStrategy createGraphStrategy(Graph innerGraph) {
        return new DefaultGraphStrategy(innerGraph, this);
    }

    public VertexStrategy createVertexStrategy(Vertex innerVertex, GraphStrategy graph) {
        return new DefaultVertexStrategy(innerVertex, graph);
    }

    public EdgeStrategy createEdgeStrategy(Edge innerEdge, GraphStrategy graph) {
        return new DefaultEdgeStrategy(innerEdge, graph);
    }

    public <V> PropertyStrategy<V> createPropertyStrategy(Property<V> innerProperty, GraphStrategy graph) {
        return new DefaultPropertyStrategy<V>(innerProperty, graph);
    }

    public <V> VertexPropertyStrategy<V> createVertexPropertyStrategy(VertexProperty<V> innerVertexProperty, GraphStrategy graph) {
        return new DefaultVertexPropertyStrategy<>(innerVertexProperty, graph);
    }

    //
    //  GRAPH
    //

    public class DefaultGraphStrategy implements GraphStrategy {

        private final Graph innerGraph;
        private final Strategy strategy;

        public DefaultGraphStrategy(final Graph innerGraph, Strategy strategy) {
            this.innerGraph = innerGraph;
            this.strategy = strategy;
        }

        public Graph getInnerGraph() {
            return this.innerGraph;
        }

        public <V extends Strategy> V getStrategy() {
            return (V)strategy;
        }
    }

    //
    //  ELEMENT
    //

    public abstract class DefaultElementStrategy implements ElementStrategy {

        protected final Element innerElement;
        protected final GraphStrategy graph;

        public DefaultElementStrategy(final Element innerElement, GraphStrategy graph) {
            this.innerElement = innerElement;
            this.graph = graph;
        }

        public Element getInnerElement() {
            return this.innerElement;
        }

        public GraphStrategy graph() {
            return this.graph;
        }
    }

    //
    //  VERTEX
    //

    public class DefaultVertexStrategy extends DefaultElementStrategy implements VertexStrategy {

        public DefaultVertexStrategy(final Element innerElement, GraphStrategy graph) {
            super(innerElement, graph);
        }

        public Vertex getInnerVertex() {
            return (Vertex)this.innerElement;
        }

    }

    //
    //  EDGE
    //

    public class DefaultEdgeStrategy extends DefaultElementStrategy implements EdgeStrategy {


        public DefaultEdgeStrategy(final Element innerElement, GraphStrategy graph) {
            super(innerElement, graph);
        }

        public Edge getInnerEdge() {
            return (Edge)this.innerElement;
        }


    }

    //
    //  VERTEX PROPERTY
    //

    public class DefaultVertexPropertyStrategy<V> extends DefaultElementStrategy implements VertexPropertyStrategy<V> {

        public DefaultVertexPropertyStrategy(final Element innerElement, GraphStrategy graph) {
            super(innerElement, graph);
        }

        public VertexProperty<V> getInnerProperty() {
            return (VertexProperty<V>)getInnerElement();
        }
    }

    //
    //  PROPERTY
    //

    public class DefaultPropertyStrategy<V> implements PropertyStrategy<V> {

        private final Property<V> innerProperty;
        private final GraphStrategy graph;

        public DefaultPropertyStrategy(final Property<V> innerProperty, GraphStrategy graph) {
            this.innerProperty = innerProperty;
            this.graph = graph;
        }

        public Property<V> getInnerProperty() {
            return this.innerProperty;
        }

        public GraphStrategy graph() {
            return this.graph;
        }
    }

}
