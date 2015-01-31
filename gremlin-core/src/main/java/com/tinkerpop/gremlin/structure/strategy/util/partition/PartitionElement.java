package com.tinkerpop.gremlin.structure.strategy.util.partition;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.ElementStrategy;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;

/**
 * Created by davidcrouch on 1/27/2015.
 */
public class PartitionElement implements ElementStrategy {

    protected PartitionStrategy strategy;
    private Element innerElement;
    private Graph graph;

    public PartitionElement(final Element innerElement, Graph graph) {
        this.innerElement = innerElement;
        this.graph = graph;
        this.strategy = graph.<PartitionStrategy>getStrategy();
    }

    public Element getInnerElement() {
        return innerElement;
    }

    public Graph graph() {
        return this.graph;
    }

}