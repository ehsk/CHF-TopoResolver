package tr.util.redis;

import redis.clients.jedis.Pipeline;

import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/5/2016
 * Time: 12:07 AM
 */
public class PipelinedSet<T> {
    private final String key;
    private final Pipeline pipeline;
    private final Function<T, String> toString;

    public PipelinedSet(String key, Pipeline pipeline, Function<T, String> toString) {
        this.key = key;
        this.pipeline = pipeline;
        this.toString = toString;
    }

    public PipelinedSet(String key, Pipeline pipeline) {
        this(key, pipeline, Object::toString);
    }

    public void add(T item) {
        pipeline.sadd(key, toString.apply(item));
    }

    public void remove(T item) {
        pipeline.srem(key, toString.apply(item));
    }
}
