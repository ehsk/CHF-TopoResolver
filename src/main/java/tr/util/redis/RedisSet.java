package tr.util.redis;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/4/2016
 * Time: 11:14 PM
 */
public class RedisSet<T> implements Iterable<T> {
    private final String key;
    private final Function<String, T> toCustomType;
    private final Function<T, String> toString;

    public static RedisSet<Long> newLongSet(String key) {
        return new RedisSet<>(key, Long::valueOf);
    }

    public static RedisSet<String> newStringSet(String key) {
        return new RedisSet<>(key, s -> s);
    }

    public static RedisSet<Integer> newIntegerSet(String key) {
        return new RedisSet<>(key, Integer::valueOf);
    }


    public RedisSet(String key, Function<String, T> toCustomType, Function<T, String> toString) {
        this.key = key;
        this.toCustomType = toCustomType;
        this.toString = toString;
    }

    public RedisSet(String key, Function<String, T> toCustomType) {
        this(key, toCustomType, Object::toString);
    }



    public boolean exists() {
        return RedisDAO.exec(jedis -> jedis.exists(key));
    }

    public long size() {
        return RedisDAO.exec(jedis -> jedis.scard(key));
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void add(T item) {
        RedisDAO.exec(jedis -> jedis.sadd(key, toString.apply(item)));
    }

    public void remove(T item) {
        RedisDAO.exec(jedis -> jedis.srem(key, toString.apply(item)));
    }

    private ScanResult<String> scan(String cursor, int pageSize) {
        return RedisDAO.exec(jedis -> jedis.sscan(key, cursor, new ScanParams().count(pageSize)));
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int currentIndex = 0;
            List<String> currentPage = null;
            String cursor = "0";

            @Override
            public boolean hasNext() {
                if (currentPage != null) {
                    if (currentIndex >= currentPage.size()) {
                        if (!cursor.equals("0")) {
                            final ScanResult<String> result = scan(cursor, 100);
                            currentPage = result.getResult();
                            cursor = result.getStringCursor();
                            currentIndex = 0;
                        }
                    }
                } else {
                    final ScanResult<String> result = scan(cursor, 100);
                    currentPage = result.getResult();
                    cursor = result.getStringCursor();
                }

                return currentIndex < currentPage.size();
            }

            @Override
            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return toCustomType.apply(currentPage.get(currentIndex++));
            }
        };
    }
}
