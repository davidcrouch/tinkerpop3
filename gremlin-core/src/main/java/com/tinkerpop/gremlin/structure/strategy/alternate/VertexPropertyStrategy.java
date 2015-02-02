package com.tinkerpop.gremlin.structure.strategy.alternate;

import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.strategy.StrategyWrapped;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedVertexProperty;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.util.Iterator;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public interface VertexPropertyStrategy<V> extends PropertyStrategy<V>, ElementStrategy, VertexProperty<V>, VertexProperty.Iterators, StrategyWrapped, WrappedVertexProperty<VertexProperty<V>> {


    public default VertexProperty getBaseVertexProperty() {
        if (getInnerProperty() instanceof StrategyWrapped)
            return ((VertexPropertyStrategy) getInnerProperty()).getBaseVertexProperty();
        else
            return getInnerProperty();
    }

    public GraphStrategy graph();

    public default VertexProperty<V> getInnerProperty() {
        return (VertexProperty<V>)getInnerElement();
    }

    /**
     * Gets the {@link com.tinkerpop.gremlin.structure.Vertex} that owns this {@code VertexProperty}.
     */
    @Override
    public default Vertex element() {
        return (Vertex)getInnerElement();
    }

    /**
     * Removes the {@code Element} from the graph.
     */
    public default void remove()  {
        getInnerElement().remove();
    }

    /**
     * Gets the label for the graph {@code Element} which helps categorize it.
     *
     * @return The label of the element
     */
    public default String label() {
        return getInnerElement().label();
    }

    /**
     * Get the graph that this element is within.
     *
     * @return the graph of this element
     */
//    public Graph graph();

    /**
     * Gets the {@link VertexProperty.Iterators} set.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public default VertexProperty.Iterators iterators() {
        return this;

    }

    @Override
    public default <U> Iterator<Property<U>> propertyIterator(final String... propertyKeys) {
        return  StreamFactory.stream(getInnerProperty().iterators().<U>propertyIterator(propertyKeys)).map(property -> (Property<U>)graph().getStrategy().createPropertyStrategy(property, graph())).iterator();
    }

}
