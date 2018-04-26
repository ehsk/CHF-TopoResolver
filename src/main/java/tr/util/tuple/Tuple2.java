package tr.util.tuple;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 3/19/2017
 * Time: 2:58 AM
 */
public class Tuple2<U, V> {
    private final U _1;
    private final V _2;

    public Tuple2(U _1, V _2) {
        this._1 = _1;
        this._2 = _2;
    }

    public U get_1() {
        return _1;
    }

    public V get_2() {
        return _2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        return Objects.equals(_1, tuple2._1) &&
                Objects.equals(_2, tuple2._2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2);
    }
}
