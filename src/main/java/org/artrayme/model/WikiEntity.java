package org.artrayme.model;

import java.util.Map;
import java.util.Objects;

public final class WikiEntity {
    private final String wikiId;
    private final Map<String, String> labels;
    private final Map<String, String> descriptions;

    public WikiEntity(String wikiId,
                      Map<String, String> labels,
                      Map<String, String> descriptions) {
        this.labels = labels;
        this.wikiId = wikiId;
        this.descriptions = descriptions;
    }

    public String wikiId() {
        return wikiId;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Map<String, String> descriptions() {
        return descriptions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wikiId, labels, descriptions);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (WikiEntity) obj;
        return Objects.equals(this.wikiId, that.wikiId) &&
                Objects.equals(this.labels, that.labels) &&
                Objects.equals(this.descriptions, that.descriptions);
    }

    @Override
    public String toString() {
        return "WikiEntity[" +
                "wikiId=" + wikiId + ", " +
                "labels=" + labels + ", " +
                "description=" + descriptions + ']';
    }
}
