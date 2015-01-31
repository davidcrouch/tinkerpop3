package com.tinkerpop.gremlin.structure.strategy.util;

import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.strategy.ElementStrategy;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.util.ElementHelper;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public abstract class DefaultElementStrategy implements ElementStrategy {

    protected final Element innerElement;
    protected final Graph graph;

    public DefaultElementStrategy(final Element innerElement, Graph graph) {
        this.innerElement = innerElement;
        this.graph = graph;
    }

    public Element getInnerElement() {
        return this.innerElement;
    }

    public Graph graph() {
        return this.graph;
    }
}
