package com.tinkerpop.gremlin.structure.strategy.util;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.strategy.VertexStrategy;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public class DefaultVertexStrategy extends DefaultElementStrategy implements VertexStrategy {

    public DefaultVertexStrategy(final Element innerElement, Graph graph) {
        super(innerElement, graph);
    }

    public Vertex getInnerVertex() {
        return (Vertex)this.innerElement;
    }

}
