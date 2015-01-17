package com.tinkerpop.gremlin.structure.util.detached;

import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.VertexProperty;
import com.tinkerpop.gremlin.structure.util.ElementHelper;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import com.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DetachedVertexProperty<V> extends DetachedElement<Property<V>> implements VertexProperty<V>, VertexProperty.Iterators {

    protected V value;
    protected transient DetachedVertex vertex;

    private DetachedVertexProperty() {
    }

    protected DetachedVertexProperty(final VertexProperty<V> vertexProperty, final boolean withProperties) {
        super(vertexProperty);
        this.value = vertexProperty.value();
        this.vertex = DetachedFactory.detach(vertexProperty.element(), false);

        if (withProperties && vertexProperty.graph().features().vertex().supportsMetaProperties()) {
            this.properties = new HashMap<>();
            vertexProperty.iterators().propertyIterator().forEachRemaining(property -> this.properties.put(property.key(), Collections.singletonList(DetachedFactory.detach(property))));
        }
    }

    public DetachedVertexProperty(final Object id, final String label, final V value,
                                  final Map<String, Object> properties,
                                  final Vertex vertex) {
        super(id, label);
        this.value = value;
        this.vertex = DetachedFactory.detach(vertex, true);

        if (!properties.isEmpty()) {
            this.properties = new HashMap<>();
            properties.entrySet().iterator().forEachRemaining(entry -> this.properties.put(entry.getKey(), Collections.singletonList(new DetachedProperty<>(entry.getKey(), entry.getValue(), this))));
        }
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public String key() {
        return this.label;
    }

    @Override
    public V value() {
        return this.value;
    }

    @Override
    public Vertex element() {
        return this.vertex;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Detached properties are readonly: " + this.toString());
    }

    @Override
    public String toString() {
        return StringFactory.propertyString(this);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(final Object object) {
        return ElementHelper.areEqual(this, object);
    }

    @Override
    public VertexProperty<V> attach(final Vertex hostVertex) {
        final Iterator<VertexProperty<V>> vertexPropertyIterator = IteratorUtils.filter(hostVertex.iterators().propertyIterator(this.label), vp -> ElementHelper.areEqual(this, vp));
        if (!vertexPropertyIterator.hasNext())
            throw new IllegalStateException("The detached vertex property could not be be found at the provided vertex: " + this);
        return vertexPropertyIterator.next();
    }

    @Override
    public VertexProperty<V> attach(final Graph hostGraph) {
        return this.attach(this.vertex.attach(hostGraph));
    }


    @Override
    public VertexProperty.Iterators iterators() {
        return this;
    }

    @Override
    public <U> Iterator<Property<U>> propertyIterator(final String... propertyKeys) {
        return (Iterator) super.propertyIterator(propertyKeys);
    }
}
