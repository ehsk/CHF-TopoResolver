package tr;

/**
 * Named Entity tags
 * <p>
 * We employ 3-label Stanford NER to tag named entities.
 * Here is a list of the tags:
 * {@link NamedEntityTag#ORGANIZATION}, {@link NamedEntityTag#PERSON},
 * {@link NamedEntityTag#LOCATION}, {@link NamedEntityTag#OTHER}
 * </p>
 *
 * @see NamedEntityTag#from(String)
 */
public enum NamedEntityTag {
    ORGANIZATION, PERSON, LOCATION, OTHER;

    /**
     * Converts a tag string to {@link NamedEntityTag} enum.
     * <p>
     * Note that checking with known tags is case-insensitive.
     * </p>
     * @param tag a tag reference to convert
     * @return the corresponding {@link NamedEntityTag}
     * @throws IllegalArgumentException if the tag is unknown
     */
    public static NamedEntityTag from(String tag) {
        try {
            return valueOf(tag);
        } catch (IllegalArgumentException e) {
            if (tag.equalsIgnoreCase("O"))
                return OTHER;

            throw e;
        }
    }

}
