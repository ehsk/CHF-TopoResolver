package tr.util;

/**
 * {@code Distance} class represents a distance values and its corresponding unit.
 * This class provides a convenient way to compare distances with different units.
 * <p>
 * The best way to acquire a {@code Distance} object is through {@link DistanceUnit}:
 * </p>
 * <blockquote><pre>
 *     Distance distance = DistanceUnit.mi.of(1.5);
 * </pre></blockquote>
 * You can also, use the constructor:
 * <blockquote><pre>
 *     Distance distance = new Distance(1.5, DistanceUnit.mi);
 * </pre></blockquote>
 * <p>
 * To convert the value to different units, functions such as {@code toMillimetres} can be called.
 * </p>
 * <blockquote><pre>
 *     double inchValue = distance.toInches();
 * </pre></blockquote>
 * <p>
 * Also, for comparison, while the class overrides {@link Comparable},
 * methods like {@code ge} and {@code gt} are provided.
 * <blockquote><pre>
 *     if (distance.ge(otherDistance))
 *          System.out.println("distance is greater");
 * </pre></blockquote>
 * </p>
 *
 * @see DistanceUnit
 */
public class Distance implements Comparable<Distance> {
    private final double value;
    private final DistanceUnit unit;

    public Distance(double value, DistanceUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public DistanceUnit getUnit() {
        return unit;
    }

    @Override
    public int compareTo(Distance o) {
        return Double.compare(toMetres(), o.toMetres());
    }

    public boolean ge(Distance o) {
        return compareTo(o) >= 0;
    }

    public boolean gt(Distance o) {
        return compareTo(o) > 0;
    }

    public boolean le(Distance o) {
        return compareTo(o) <= 0;
    }

    public boolean lt(Distance o) {
        return compareTo(o) < 0;
    }

    public double toMillimetres() {
        return unit.toMillimetres(value);
    }

    public double toCentimetres() {
        return unit.toCentimetres(value);
    }

    public double toMetres() {
        return unit.toMetres(value);
    }

    public double toKilometres() {
        return unit.toKilometres(value);
    }

    public double toInches() {
        return unit.toInches(value);
    }

    public double toFeet() {
        return unit.toFeet(value);
    }

    public double toYards() {
        return unit.toYards(value);
    }

    public double toMiles() {
        return unit.toMiles(value);
    }
}
