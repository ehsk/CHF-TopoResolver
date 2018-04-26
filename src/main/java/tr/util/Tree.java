package tr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/24/2017
 * Time: 2:36 PM
 */
public class Tree<T> {
    private final T element;

    private Tree<T> parent;
    private final List<Tree<T>> children = new ArrayList<>();

    public Tree(T element) {
        this.element = element;
    }

    @Override
    public String toString() {
        return element.toString();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public Tree<T> getDummy() {
        final Tree<T> dummy = new Tree<>(null);
        dummy.getChildren().add(this);
        return dummy;
    }

    public String preorder(String prefix, String suffix, Function<T, String> toStringFunction) {
        return prefix +
                toStringFunction.apply(this.getElement()) +
                children.stream()
                        .map(ch -> ch.preorder(prefix, suffix, toStringFunction))
                        .collect(Collectors.joining("")) +
                suffix;
    }

    public void traversePreorder(Consumer<Tree<T>> consumer) {
        consumer.accept(this);
        for (Tree<T> child : children) {
            child.traversePreorder(consumer);
        }
    }

    public void traverseToRoot(Consumer<Tree<T>> consumer) {
        consumer.accept(this);
        Optional.ofNullable(this.parent).ifPresent(p -> p.traverseToRoot(consumer));
    }

    public List<Tree<T>> filter(Predicate<Tree<T>> predicate) {
        final List<Tree<T>> result = new ArrayList<>();

        if (predicate.test(this))
            result.add(this);

        for (Tree<T> child : children) {
            result.addAll(child.filter(predicate));
        }

        return result;
    }

    public long size() {
        if (isLeaf())
            return 1;

        LongSummaryStatistics acc = new LongSummaryStatistics();
        for (Tree<T> child : children) {
            acc.accept(child.size());
        }

        return acc.getSum() + 1;
    }

    public Tree(T element, Tree<T> parent) {
        this.element = element;

        this.parent = parent;
        if (this.parent != null)
            this.parent.getChildren().add(this);
    }

    public T getElement() {
        return element;
    }

    public Tree<T> getParent() {
        return parent;
    }

    public void setParent(Tree<T> parent) {
        this.parent = parent;
    }

    public List<Tree<T>> getChildren() {
        return children;
    }
}
