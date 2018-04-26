package tr;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 10/14/2016
 * Time: 1:30 PM
 */
public class TaggedWord {
    private final String phrase;
    private final NamedEntityTag tag;
    private int index;
    private final int start, end;

    public TaggedWord(String phrase, String tag, int index, int start, int end) {
        this.phrase = phrase;
        this.tag = NamedEntityTag.from(tag);
        this.index = index;
        this.start = start;
        this.end = end;
    }

    public TaggedWord(String phrase, NamedEntityTag tag, int index, int start, int end) {
        this.phrase = phrase;
        this.tag = tag;
        this.index = index;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        TaggedWord that = (TaggedWord) o;
        return start == that.start &&
                end == that.end &&
                phrase.equalsIgnoreCase(that.phrase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phrase.toLowerCase(), start, end);
    }

    public boolean match(final Toponym toponym) {
        return phrase.equalsIgnoreCase(toponym.getPhrase()) && start == toponym.getStart() && end == toponym.getEnd();
    }

    @Override
    public String toString() {
        return String.format("%s/%s %d[%d-%d]", phrase, tag, index, start, end);
    }

    public String getPhrase() {
        return phrase;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public NamedEntityTag getTag() {
        return tag;
    }

}
