package tr.util.redis;

import tr.util.StringUtil;

import java.util.*;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/4/2016
 * Time: 11:14 PM
 */
public class RedisHash<T> {
    private final String key;
    private final Function<String, T> toCustomType;
    private final Function<T, String> toString;

    public static RedisHash<Double> newDoubleSet(String key) {
        return new RedisHash<>(key, Double::valueOf);
    }

    public static RedisHash<Long> newLongSet(String key) {
        return new RedisHash<>(key, Long::valueOf);
    }

    public static RedisHash<String> newStringSet(String key) {
        return new RedisHash<>(key, s -> s);
    }

    public static RedisHash<Integer> newIntegerSet(String key) {
        return new RedisHash<>(key, Integer::valueOf);
    }


    public RedisHash(String key, Function<String, T> toCustomType, Function<T, String> toString) {
        this.key = key;
        this.toCustomType = toCustomType;
        this.toString = toString;
    }

    public RedisHash(String key, Function<String, T> toCustomType) {
        this(key, toCustomType, Object::toString);
    }

    public boolean exists() {
        return RedisDAO.exec(jedis -> jedis.exists(key));
    }

    public long size() {
        return RedisDAO.exec(jedis -> jedis.hlen(key));
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void set(String field, T item) {
        RedisDAO.exec(jedis -> jedis.hset(key, field, toString.apply(item)));
    }

    public void set(Map<String, T> fields) {
        Map<String, String> redisFeilds = new HashMap<>();
        fields.forEach((field, value) -> redisFeilds.put(field, toString.apply(value)));

        RedisDAO.exec(jedis -> jedis.hmset(key, redisFeilds));
    }

    public boolean contains(String field) {
        return RedisDAO.exec(jedis -> jedis.hexists(key, field));
    }

    public Optional<T> get(String field) {
        final String result = RedisDAO.exec(jedis -> jedis.hget(key, field));
        return StringUtil.hasText(result) ? Optional.of(toCustomType.apply(result)) : Optional.empty();
    }

    public T getOrDefault(String field, T defaultValue) {
        return get(field).orElse(defaultValue);
    }

    public void remove(String... fields) {
        RedisDAO.exec(jedis -> jedis.hdel(key, fields));
    }

    public Map<String, T> get(String... fields) {
        if (fields == null)
            return Collections.emptyMap();

        final List<String> resp = RedisDAO.exec(jedis -> jedis.hmget(key, fields));
        final Map<String, T> mappedResp = new HashMap<>();

        for (int i = 0; i < resp.size(); i++) {
            mappedResp.put(fields[i], toCustomType.apply(resp.get(i)));
        }

        return mappedResp;
    }

    public Map<String, T> getAll() {
        final Map<String, String> resp = RedisDAO.exec(jedis -> jedis.hgetAll(key));

        final Map<String, T> mappedResp = new HashMap<>();
        resp.forEach((field, value) -> mappedResp.put(field, toCustomType.apply(value)));

        return mappedResp;
    }
}
