package com.tinkerpop.gremlin.structure.strategy.util;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Transaction;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.strategy.Strategy;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedGraph;
import com.tinkerpop.gremlin.util.function.FunctionUtils;
import org.apache.commons.configuration.Configuration;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Created by davidcrouch on 1/26/2015.
 */
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
