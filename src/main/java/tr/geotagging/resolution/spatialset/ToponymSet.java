package tr.geotagging.resolution.spatialset;

import tr.geonames.GeoNamesEntry;
import tr.geotagging.resolution.GeoCandidateEntry;
import tr.util.Tree;
import tr.util.tuple.Tuple2;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/1/2017
 * Time: 4:52 PM
 */
class ToponymSet {
    private Tree<Element> pivot;
    private Tree<Element> root;

    private double cost = 1;

    ToponymSet(String toponymText, GeoCandidateEntry candidateEntry) {
        Tree<Element> parent = null;

        for (GeoNamesEntry entry : candidateEntry) {
            if (entry.equals(candidateEntry.getGeoNamesCandid())) {
                this.pivot = parent;
            }

            if (parent == null) {
                this.root = new Tree<>(new Element(entry));
                parent = this.root;
            } else {
                parent = new Tree<>(new Element(entry), parent);
            }

        }

        if (parent != null) {
            parent.getElement().toponyms.add(toponymText);
        }

        if (this.pivot == null)
            this.pivot = this.root;
    }

    public Set<String> toponyms() {
        Set<String> allToponyms = new HashSet<>();
        pivot.traversePreorder(n -> allToponyms.addAll(n.getElement().toponyms));
        return allToponyms;
    }

    public long getPopulation() {
        LongSummaryStatistics acc = new LongSummaryStatistics();
        pivot.traversePreorder(n -> {
            if (!n.getElement().toponyms.isEmpty())
                acc.accept(n.getElement().entry.getPopulation());
        });

        return acc.getSum();
    }

    boolean contains(GeoNamesEntry targetEntry) {
        return !pivot.filter(n -> n.getElement().entry.equals(targetEntry)).isEmpty();
    }

    @Override
    public String toString() {
        return pivot.toString();
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    Tree<Element> getPivot() {
        return pivot;
    }

    public Tree<Element> getRoot() {
        return root;
    }

    void addChild(String toponymText, GeoCandidateEntry candidateEntry) {
        final Element newElement = new Element(candidateEntry.getGeoNamesCandid());
        newElement.toponyms.add(toponymText);

        new Tree<>(newElement, this.pivot);
    }

    Optional<Element> pickPopulatedNode(String toponymText) {
        Tuple2<Long, Element> best = new Tuple2<>(Long.MIN_VALUE, null);
        for (Tree<Element> matchedNode : pivot.filter(n -> n.getElement().toponyms.contains(toponymText))) {
            if (best.get_1() < matchedNode.getElement().entry.getPopulation())
                best = new Tuple2<>(matchedNode.getElement().entry.getPopulation(), matchedNode.getElement());
        }

        return best.get_2() != null ? Optional.of(best.get_2()) : Optional.empty();
    }

    public Optional<Element> find(GeoNamesEntry targetEntry) {
        final List<Tree<Element>> result = pivot.filter(n -> n.getElement().entry.equals(targetEntry));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0).getElement());
    }

    class Element {
        final GeoNamesEntry entry;
        final Set<String> toponyms;

        Element(GeoNamesEntry entry) {
            this.entry = entry;
            this.toponyms = new HashSet<>();
        }

        @Override
        public String toString() {
            return entry.toString() + "/" + entry.getLevel();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && entry.equals(((Element) o).entry);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entry);
        }

        public boolean equals(GeoNamesEntry anotherEntry) {
            return equals(anotherEntry.getGeonameId());
        }

        public boolean equals(Long geonameId) {
            return entry.getGeonameId().equals(geonameId);
        }
    }

}
