package com.tinkerpop.gremlin.structure.strategy.alternate;

import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.strategy.StrategyWrapped;
import com.tinkerpop.gremlin.util.StreamFactory;

import java.util.*;

/**
 * Created by davidcrouch on 1/26/2015.
 */
public interface ElementStrategy extends Element, Element.Iterators, StrategyWrapped {

    public default Element getBaseElement() {
        if (this.getInnerElement() instanceof StrategyWrapped)
            return ((ElementStrategy)getInnerElement()).getBaseElement();
        else
            return this.getInnerElement();
    }

    public Element getInnerElement();

    public GraphStrategy graph();


    /**
     * Gets the unique identifier for the graph {@code Element}.
     *
     * @return The id of the element
     */
    public default Object id() {
        return getInnerElement().id();
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
     * Add or set a property value for the {@code Element} given its key.
     */
    public default <V> Property<V> property(final String key, final V value) {
        return graph().getStrategy().createPropertyStrategy(getInnerElement().property(key, value), graph());
    }


    /**
     * Removes the {@code Element} from the graph.
     */
    public default void remove()  {
        getInnerElement().remove();
    }

    /**
     * Gets the iterators for the {@code Element}.  Iterators provide low-level access to the data associated with
     * an {@code Element} as they do not come with the overhead of {@link com.tinkerpop.gremlin.process.Traversal}
     * construction.  Use iterators in places where performance is most crucial.
     */
    public default Element.Iterators iterators() {
        return this;
    }

    /**
     * Get an {@link Iterator} of properties.
     */
    public default <V> Iterator<? extends Property<V>> propertyIterator(final String... propertyKeys) {
        return  StreamFactory.stream(getInnerElement().iterators().<V>propertyIterator(propertyKeys)).map(property -> graph().getStrategy().createPropertyStrategy(property, graph())).iterator();
    }

}
