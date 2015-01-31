package com.tinkerpop.gremlin.structure.strategy.util;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.EdgeStrategy;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public class DefaultEdgeStrategy extends DefaultElementStrategy implements EdgeStrategy {


    public DefaultEdgeStrategy(final Element innerElement, Graph graph) {
        super(innerElement, graph);
    }

    public Edge getInnerEdge() {
        return (Edge)this.innerElement;
    }


}
