package org.artrayme;

import java.awt.Image;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WikiEntity {
    private final String idtf;
    private final Map<String, String> labels;
    private final Map<String, String> description;

    public WikiEntity(String idtf,
                      Map<String, String> labels,
                      Map<String, String> description) {
        if (labels.isEmpty()) throw new RuntimeException("There is entity without labels -- " + idtf + ". " +
                "You can add labels for this entity in Wikidata, or you can change input keywords");
        this.labels = labels;
        this.idtf = idtf;
        this.description = description;
    }

    public String idtf() {
        return idtf;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Map<String, String> description() {
        return description;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (WikiEntity) obj;
        return Objects.equals(this.idtf, that.idtf) &&
                Objects.equals(this.labels, that.labels) &&
                Objects.equals(this.description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idtf, labels, description);
    }

    @Override
    public String toString() {
        return "WikiEntity[" +
                "idtf=" + idtf + ", " +
                "labels=" + labels + ", " +
                "description=" + description + ']';
    }
}
