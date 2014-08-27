package com.tinkerpop.gremlin.tinkergraph.process.computer;

import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.computer.MapReduce;
import com.tinkerpop.gremlin.process.computer.Memory;
import com.tinkerpop.gremlin.process.computer.VertexProgram;
import com.tinkerpop.gremlin.process.computer.util.MemoryHelper;
import com.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
// TODO: add TinkerASPMemory
public class TinkerMemory implements Memory.Administrative {

    public final Set<String> memoryKeys = new HashSet<>();
    public Map<String, Object> previousMap;
    public Map<String, Object> currentMap;
    private final AtomicInteger iteration = new AtomicInteger(0);
    private final AtomicLong runtime = new AtomicLong(0l);
    private boolean complete = false;

    public TinkerMemory(final VertexProgram vertexProgram, final List<MapReduce> mapReducers) {
        this.currentMap = new ConcurrentHashMap<>();
        this.previousMap = new ConcurrentHashMap<>();
        if (null != vertexProgram) {
            for (final String key : (Set<String>) vertexProgram.getMemoryComputeKeys()) {
                MemoryHelper.validateKey(key);
                this.memoryKeys.add(key);
            }
        }
        for (final MapReduce mapReduce : mapReducers) {
            this.memoryKeys.add(mapReduce.getSideEffectKey());
        }
    }

    public Set<String> keys() {
        return this.previousMap.keySet();
    }

    public void incrIteration() {
        this.iteration.getAndIncrement();
    }

    public int getIteration() {
        return this.iteration.get();
    }

    public void setRuntime(final long runTime) {
        if (this.complete) throw Memory.Exceptions.memoryCompleteAndImmutable();
        this.runtime.set(runTime);
    }

    public long getRuntime() {
        return this.runtime.get();
    }

    protected void complete() {
        this.iteration.decrementAndGet();
        this.complete = true;
        this.previousMap = this.currentMap;
    }

    protected void completeSubRound() {
        this.previousMap = new ConcurrentHashMap<>(this.currentMap);

    }

    public boolean isInitialIteration() {
        return this.getIteration() == 0;
    }

    public <R> R get(final String key) throws IllegalArgumentException {
        final R r = (R) this.previousMap.get(key);
        if (null == r)
            throw Memory.Exceptions.memoryDoesNotExist(key);
        else
            return r;
    }

    public long incr(final String key, final long delta) {
        checkKeyValue(key, delta);
        final Long currentValue = (Long) this.currentMap.getOrDefault(key, 0l);
        this.currentMap.put(key, delta + currentValue);

        final Long previousValue = (Long) this.previousMap.getOrDefault(key, 0l);
        return previousValue + delta;
    }

    public boolean and(final String key, final boolean bool) {
        checkKeyValue(key, bool);
        final Boolean currentValue = (Boolean) this.currentMap.getOrDefault(key, true);
        this.currentMap.put(key, bool && currentValue);

        final Boolean previousValue = (Boolean) this.previousMap.getOrDefault(key, true);
        return previousValue && bool;
    }

    public boolean or(final String key, final boolean bool) {
        checkKeyValue(key, bool);
        final Boolean currentValue = (Boolean) this.currentMap.getOrDefault(key, true);
        this.currentMap.put(key, bool || currentValue);

        final Boolean previousValue = (Boolean) this.previousMap.getOrDefault(key, true);
        return previousValue || bool;
    }

    public void set(final String key, final Object value) {
        checkKeyValue(key, value);
        this.currentMap.put(key, value);
    }

    public String toString() {
        return StringFactory.computeMemoryString(this);
    }

    private void checkKeyValue(final String key, final Object value) {
        if (this.complete) throw Memory.Exceptions.memoryCompleteAndImmutable();
        if (!this.memoryKeys.contains(key))
            throw GraphComputer.Exceptions.providedKeyIsNotAMemoryKey(key);
        MemoryHelper.validateValue(value);
    }
}