package tr.util;

import java.util.function.ToDoubleBiFunction;

/**
 * A Java 8 {@link ToDoubleBiFunction function} to compute the distance between two objects.
 * Here, it is assumed the objects own similar types.
 *
 * @param <T> the type of the object to calculate distance
 */
public interface DistanceFunction<T> extends ToDoubleBiFunction<T, T> {
}
