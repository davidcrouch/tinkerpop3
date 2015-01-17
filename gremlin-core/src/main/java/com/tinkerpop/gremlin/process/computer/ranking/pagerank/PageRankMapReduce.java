package com.tinkerpop.gremlin.process.computer.ranking.pagerank;

import com.tinkerpop.gremlin.process.computer.KeyValue;
import com.tinkerpop.gremlin.process.computer.util.StaticMapReduce;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.commons.configuration.Configuration;

import java.util.Iterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class PageRankMapReduce extends StaticMapReduce<Object, Double, Object, Double, Iterator<KeyValue<Object, Double>>> {

    public static final String PAGE_RANK_MEMORY_KEY = "gremlin.pageRankMapReduce.memoryKey";
    public static final String DEFAULT_MEMORY_KEY = "pageRank";

    private String memoryKey = DEFAULT_MEMORY_KEY;

    private PageRankMapReduce() {

    }

    private PageRankMapReduce(final String memoryKey) {
        this.memoryKey = memoryKey;
    }

    @Override
    public void storeState(final Configuration configuration) {
        super.storeState(configuration);
        configuration.setProperty(PAGE_RANK_MEMORY_KEY, this.memoryKey);
    }

    @Override
    public void loadState(final Configuration configuration) {
        this.memoryKey = configuration.getString(PAGE_RANK_MEMORY_KEY, DEFAULT_MEMORY_KEY);
    }

    @Override
    public boolean doStage(final Stage stage) {
        return stage.equals(Stage.MAP);
    }

    @Override
    public void map(final Vertex vertex, final MapEmitter<Object, Double> emitter) {
        final Property pageRank = vertex.property(PageRankVertexProgram.PAGE_RANK);
        if (pageRank.isPresent()) {
            emitter.emit(vertex.id(), (Double) pageRank.value());
        }
    }

    @Override
    public Iterator<KeyValue<Object, Double>> generateFinalResult(final Iterator<KeyValue<Object, Double>> keyValues) {
        return keyValues;
    }

    @Override
    public String getMemoryKey() {
        return this.memoryKey;
    }

    @Override
    public String toString() {
        return StringFactory.mapReduceString(this, this.memoryKey);
    }

    //////////////////////////////

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {

        private String memoryKey = DEFAULT_MEMORY_KEY;

        private Builder() {

        }

        public Builder memoryKey(final String memoryKey) {
            this.memoryKey = memoryKey;
            return this;
        }

        public PageRankMapReduce create() {
            return new PageRankMapReduce(this.memoryKey);
        }

    }
}