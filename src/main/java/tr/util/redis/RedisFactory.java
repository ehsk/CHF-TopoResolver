package tr.util.redis;

import tr.util.Config;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/4/2016
 * Time: 11:06 PM
 */
public class RedisFactory {
    private final JedisPool jedisPool;

    private RedisFactory() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(Config.Redis.MAX_CONNECTIONS);
        jedisPool = new JedisPool(poolConfig, Config.Redis.HOST, Config.Redis.PORT, Config.Redis.TIMEOUT);
    }

    private static final RedisFactory factory = new RedisFactory();

    public static Jedis getWorker() {
        return factory.jedisPool.getResource();
    }

}
