package tr.util.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/4/2016
 * Time: 11:14 PM
 */
public class RedisDAO {
    public static <T> T pipeline(final Function<Pipeline, T> f) {
        return exec(jedis -> {
            final Pipeline pl = jedis.pipelined();
            final T result = f.apply(pl);
            pl.sync();
            return result;
        });

    }

    public static <T> T exec(Function<Jedis, T> f) {
        try (final Jedis jedis = RedisFactory.getWorker()) {
            return f.apply(jedis);
        }
    }
}
