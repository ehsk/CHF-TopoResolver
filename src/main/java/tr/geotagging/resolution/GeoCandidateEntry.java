package tr.geotagging.resolution;

import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesLevel;
import tr.geonames.GeoNamesUtil;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/28/2017
 * Time: 11:39 AM
 */
public class GeoCandidateEntry implements Iterable<GeoNamesEntry> {
    private final GeoNamesEntry geoNamesCandid;
    private final Map<GeoNamesLevel, GeoNamesEntry> hierarchyMap;

    GeoCandidateEntry(GeoNamesEntry geoNamesCandid, Map<GeoNamesLevel, GeoNamesEntry> hierarchyMap) {
        this.geoNamesCandid = geoNamesCandid;
        this.hierarchyMap = hierarchyMap;
    }

    public Optional<GeoNamesEntry> getEntryAt(GeoNamesLevel level) {
        final Optional<GeoNamesEntry> entry = Optional.ofNullable(hierarchyMap.get(level));
        if (entry.isPresent())
            return entry;

        return level == geoNamesCandid.getLevel() ?
                Optional.of(geoNamesCandid) : Optional.empty();
    }

    public GeoNamesEntry getParentEntry() {
        for (GeoNamesLevel lvl = geoNamesCandid.getLevel().getParent().orElse(null); lvl != null; lvl = lvl.getParent().orElse(null)) {
            final Optional<GeoNamesEntry> entryAt = getEntryAt(lvl);
            if (entryAt.isPresent())
                return entryAt.get();
        }

        return GeoNamesUtil.EARTH;
    }

    private Optional<GeoNamesEntry> getChildEntry(final GeoNamesLevel level) {
        for (Optional<GeoNamesLevel> childLevel = level.getChild(); childLevel.isPresent(); childLevel = childLevel.get().getChild()) {
            final Optional<GeoNamesEntry> entryAt = Optional.ofNullable(hierarchyMap.get(childLevel.get()));
            if (entryAt.isPresent())
                return entryAt;
            else if (geoNamesCandid.getLevel() == childLevel.get())
                return Optional.of(geoNamesCandid);
        }

        return Optional.empty();
    }

    private Optional<GeoNamesEntry> getChildEntry(final GeoNamesEntry entry) {
        return getChildEntry(entry.getLevel());
    }

    private GeoNamesEntry getRootEntry() {
        if (geoNamesCandid.getLevel() == GeoNamesLevel.COUNTRY)
            return GeoNamesUtil.EARTH;
        else {
            for (GeoNamesLevel lvl = GeoNamesLevel.EARTH; lvl != null; lvl = lvl.getChild().orElse(null)) {
                final Optional<GeoNamesEntry> entryAt = getEntryAt(lvl);
                if (entryAt.isPresent())
                    return entryAt.get();
            }

            return geoNamesCandid;
        }
    }

    public GeoNamesEntry getGeoNamesCandid() {
        return geoNamesCandid;
    }

    public Map<GeoNamesLevel, GeoNamesEntry> getHierarchyMap() {
        return hierarchyMap;
    }

    public boolean contains(GeoNamesEntry entry) {
        return geoNamesCandid.getGeonameId().equals(entry.getGeonameId()) ||
                hierarchyMap.values().stream().filter(h -> h.getGeonameId().equals(entry.getGeonameId())).count() > 0;
    }

    @Nonnull
    @Override
    public Iterator<GeoNamesEntry> iterator() {
        return new Iterator<GeoNamesEntry>() {
            GeoNamesEntry nextEntry = getRootEntry();

            @Override
            public boolean hasNext() {
                return this.nextEntry != null;
            }

            @Override
            public GeoNamesEntry next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                GeoNamesEntry current = this.nextEntry;

                this.nextEntry = getChildEntry(this.nextEntry).orElse(null);

                return current;
            }
        };
    }


}
