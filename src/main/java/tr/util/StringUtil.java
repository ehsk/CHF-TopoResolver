package tr.util;

import javax.annotation.Nonnull;

import java.util.Optional;

/**
 * A lightweight utility class to facilitate working with strings.
 */
public class StringUtil {
    private StringUtil() {
    }

    /**
     * Checks if a string is null or empty.
     * <p>
     * Similar to isNullOrEmpty method in Guava's {@link com.google.common.base.Strings}
     * </p>
     *
     * @param str a string reference to check
     * @return {@code true} if the input is not null and not empty
     */
    public static boolean hasText(String str) {
        return !Optional.ofNullable(str).orElse("").equals("");
    }

    /**
     * For a regular expression pattern, this method adds
     * an espace sequence (i.e., a backslash {@code \})
     * for special characters (i.e., {@code * - + &}).
     * <p>
     * Note that the method does not support the complete
     * list of special characters.
     * </p>
     *
     * @param str a regular expression pattern (should be not null)
     * @return the espaced pattern
     */
    public static String escRegex(@Nonnull String str) {
        return str.replaceAll("([\\*\\-+\\&])", "\\$1");
    }
}
