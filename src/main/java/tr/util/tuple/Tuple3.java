package tr.util.tuple;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 3/19/2017
 * Time: 2:58 AM
 */
public class Tuple3<U, V, W> {
    private final U _1;
    private final V _2;
    private final W _3;

    public Tuple3(U _1, V _2, W _3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = _3;
    }

    public Tuple3(Tuple2<U, V> t2, W _3) {
        this(t2.get_1(), t2.get_2(), _3);
    }

    public Tuple3(U _1, Tuple2<V, W> t2) {
        this(_1, t2.get_1(), t2.get_2());
    }

    public U get_1() {
        return _1;
    }

    public V get_2() {
        return _2;
    }

    public W get_3() {
        return _3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return Objects.equals(_1, tuple3._1) &&
                Objects.equals(_2, tuple3._2) &&
                Objects.equals(_3, tuple3._3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3);
    }
}
