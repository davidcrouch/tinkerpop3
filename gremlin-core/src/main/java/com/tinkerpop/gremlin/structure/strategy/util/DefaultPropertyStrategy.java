package com.tinkerpop.gremlin.structure.strategy.util;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.GraphStrategy;
import com.tinkerpop.gremlin.structure.strategy.PropertyStrategy;
import com.tinkerpop.gremlin.structure.strategy.StrategyWrapped;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedProperty;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public class DefaultPropertyStrategy<V> implements PropertyStrategy<V> {

    private final Property<V> innerProperty;
    private final Graph graph;

    public DefaultPropertyStrategy(final Property<V> innerProperty, Graph graph) {
        this.innerProperty = innerProperty;
        this.graph = graph;
    }

    public Property<V> getInnerProperty() {
        return this.innerProperty;
    }

    public Graph graph() {
        return this.graph;
    }
}
