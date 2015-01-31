package com.tinkerpop.gremlin.structure.strategy;

import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.util.empty.EmptyProperty;
import com.tinkerpop.gremlin.structure.util.wrapped.WrappedProperty;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public interface PropertyStrategy<V> extends Property<V>, StrategyWrapped, WrappedProperty<Property<V>> {


    public default Property<V> getBaseProperty() {
        if (getInnerProperty() instanceof StrategyWrapped)
            return ((PropertyStrategy<V>)getInnerProperty()).getBaseProperty();
        else
            return getInnerProperty();
    }

    public Property<V> getInnerProperty();

    /**
     * Get the graph that this element is within.
     *
     * @return the graph of this element
     */
//    public Graph graph();

    /**
     * The key of the property.
     *
     * @return The property key
     */
    public default String key() {
        return getInnerProperty().key();
    }

    /**
     * The value of the property.
     *
     * @return The property value
     * @throws java.util.NoSuchElementException thrown if the property is empty
     */
    public default V value() throws NoSuchElementException {
        return getInnerProperty().value();
    }

    /**
     * Whether the property is empty or not.
     *
     * @return True if the property exists, else false
     */
    public default boolean isPresent() {
        return getInnerProperty().isPresent();
    }


    /**
     * Get the element that this property is associated with.
     *
     * @return The element associated with this property (i.e. {@link com.tinkerpop.gremlin.structure.Vertex}, {@link com.tinkerpop.gremlin.structure.Edge}, or {@link com.tinkerpop.gremlin.structure.VertexProperty}).
     */
    public default Element element() {
        return getInnerProperty().element();
    }

    /**
     * Remove the property from the associated element.
     */
    public default void remove() {
        getInnerProperty().remove();
    }


}
