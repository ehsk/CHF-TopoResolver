package tr.util;

/**
 * {@code DistanceUnit} represents the distance units
 * and implicit conversions between them.
 * <p>
 * The supported units are:
 * miles (<strong>mi</strong>), kilo-meter (<strong>km</strong>), meter (<strong>m</strong>),
 * yard (<strong>yd</strong>), foot (<strong>ft</strong>), inch (<strong>in</strong>),
 * centimeter (<strong>cm</strong>), millimeter (<strong>mm</strong>).
 * </p>
 * <p>
 * To use this class, after selecting the desired unit,
 * by calling the method {@code of}, a {@link Distance} object can be obtained.
 * </p>
 * <blockquote><pre>
 *     Distance distance = DistanceUnit.mi.of(1.5);
 * </pre></blockquote>
 *
 * @see Distance
 */
public enum DistanceUnit {
    mm(0.001), cm(0.01), m(1), km(1000),
    in(0.0254), ft(0.3048), yd(0.9144), mi(1609.344);

    private final double coef;

    DistanceUnit(double coef) {
        this.coef = coef;
    }

    public Distance of(double d) {
        return new Distance(d, this);
    }

    public Distance of(Distance d) {
        return new Distance(d.toMetres() / this.coef, this);
    }

    public double toMillimetres(double d) {
        return d * coef / mm.coef;
    }

    public double toCentimetres(double d) {
        return d * coef / cm.coef;
    }

    public double toMetres(double d) {
        return d * coef;
    }

    public double toKilometres(double d) {
        return d * coef / km.coef;
    }

    public double toInches(double d) {
        return d * coef / in.coef;
    }

    public double toFeet(double d) {
        return d * coef / ft.coef;
    }

    public double toYards(double d) {
        return d * coef / yd.coef;
    }

    public double toMiles(double d) {
        return d * coef / mi.coef;
    }
}
