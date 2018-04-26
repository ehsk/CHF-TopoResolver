package tr.util.redis;

import tr.util.StringUtil;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by kamalloo on 10/30/17.
 */
public class RedisKey<T> {
    private final Function<String, T> toCustomType;
    private final Function<T, String> toString;

    public static RedisKey<Double> newDoubleSet() {
        return new RedisKey<>(Double::valueOf);
    }

    public static RedisKey<Long> newLongSet() {
        return new RedisKey<>(Long::valueOf);
    }

    public static RedisKey<String> newStringSet() {
        return new RedisKey<>(s -> s);
    }

    public RedisKey(Function<String, T> toCustomType, Function<T, String> toString) {
        this.toCustomType = toCustomType;
        this.toString = toString;
    }

    public RedisKey(Function<String, T> toCustomType) {
        this(toCustomType, Object::toString);
    }

    public boolean exists(String key) {
        return RedisDAO.exec(jedis -> jedis.exists(key));
    }

    public Long delete(String key) {
        return RedisDAO.exec(jedis -> jedis.del(key));
    }

    public void expireAfter(String key, int duration, TimeUnit unit) {
        RedisDAO.exec(jedis -> jedis.expire(key, (int) unit.toSeconds(duration)));
    }

    public void set(String key, T val) {
        RedisDAO.exec(jedis -> jedis.set(key, toString.apply(val)));
    }

    public Optional<T> get(String key) {
        final String result = RedisDAO.exec(jedis -> jedis.get(key));
        return StringUtil.hasText(result) ? Optional.of(toCustomType.apply(result)) : Optional.empty();
    }

    public T getOrDefault(String key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

}
