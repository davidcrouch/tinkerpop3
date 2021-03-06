package com.tinkerpop.gremlin.process.graph.step.filter;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.marker.Reversible;
import com.tinkerpop.gremlin.process.traverser.TraverserRequirement;
import com.tinkerpop.gremlin.process.util.TraversalHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class ExceptStep<S> extends FilterStep<S> implements Reversible {

    private final String collectionSideEffectKey;

    public ExceptStep(final Traversal traversal, final String collectionSideEffectKey) {
        super(traversal);
        this.collectionSideEffectKey = collectionSideEffectKey;
        this.setPredicate(traverser -> {
            final Object except = traverser.asAdmin().getSideEffects().exists(this.collectionSideEffectKey) ? traverser.sideEffects(this.collectionSideEffectKey) : traverser.path(this.collectionSideEffectKey);
            return except instanceof Collection ?
                    !((Collection) except).contains(traverser.get()) :
                    !except.equals(traverser.get());
        });
    }

    public ExceptStep(final Traversal traversal, final Collection<S> exceptionCollection) {
        super(traversal);
        this.collectionSideEffectKey = null;
        this.setPredicate(traverser -> !exceptionCollection.contains(traverser.get()));
    }

    public ExceptStep(final Traversal traversal, final S exceptionObject) {
        super(traversal);
        this.collectionSideEffectKey = null;
        this.setPredicate(traverser -> !exceptionObject.equals(traverser.get()));
    }

    public String toString() {
        return TraversalHelper.makeStepString(this, this.collectionSideEffectKey);
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return this.getTraversal().asAdmin().getSideEffects().exists(this.collectionSideEffectKey) ?
                Collections.singleton(TraverserRequirement.SIDE_EFFECTS) :
                Collections.singleton(TraverserRequirement.PATH_ACCESS);
    }
}
