package com.tinkerpop.gremlin.hadoop.structure;

import com.tinkerpop.gremlin.hadoop.Constants;
import com.tinkerpop.gremlin.hadoop.structure.io.VertexWritable;
import com.tinkerpop.gremlin.util.StreamFactory;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapreduce.InputFormat;
import org.javatuples.Pair;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class HadoopConfiguration extends BaseConfiguration implements Serializable, Iterable {

    public HadoopConfiguration() {

    }

    public HadoopConfiguration(final Configuration configuration) {
        this.copy(configuration);
    }

    public Class<InputFormat<NullWritable, VertexWritable>> getGraphInputFormat() {
        try {
            return (Class) Class.forName(this.getString(Constants.GREMLIN_HADOOP_GRAPH_INPUT_FORMAT));
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void setGraphInputFormat(final Class<InputFormat<NullWritable, VertexWritable>> inputFormatClass) {
        this.setProperty(Constants.GREMLIN_HADOOP_GRAPH_INPUT_FORMAT, inputFormatClass);
    }

    public Class<OutputFormat<NullWritable, VertexWritable>> getGraphOutputFormat() {
        try {
            return (Class) Class.forName(this.getString(Constants.GREMLIN_HADOOP_GRAPH_OUTPUT_FORMAT));
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void setGraphOutputFormat(final Class<OutputFormat<NullWritable, VertexWritable>> outputFormatClass) {
        this.setProperty(Constants.GREMLIN_HADOOP_GRAPH_OUTPUT_FORMAT, outputFormatClass);
    }

    public String getInputLocation() {
        return this.getString(Constants.GREMLIN_HADOOP_INPUT_LOCATION);
    }

    public void setInputLocation(final String inputLocation) {
        this.setProperty(Constants.GREMLIN_HADOOP_INPUT_LOCATION, inputLocation);
    }

    public String getOutputLocation() {
        return this.getString(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION);
    }

    public void setOutputLocation(final String outputLocation) {
        this.setProperty(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, outputLocation);
    }

    @Override
    public Iterator iterator() {
        return StreamFactory.stream(this.getKeys()).map(k -> new Pair(k, this.getProperty(k))).iterator();
    }
}