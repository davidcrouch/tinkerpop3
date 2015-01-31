package com.tinkerpop.gremlin.structure.strategy.util;

import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.VertexProperty;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.strategy.VertexPropertyStrategy;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public class DefaultVertexPropertyStrategy<V> extends DefaultElementStrategy implements VertexPropertyStrategy<V> {

    public DefaultVertexPropertyStrategy(final Element innerElement, Graph graph) {
        super(innerElement, graph);
    }

    public VertexProperty<V> getInnerProperty() {
        return (VertexProperty<V>)getInnerElement();
    }
}