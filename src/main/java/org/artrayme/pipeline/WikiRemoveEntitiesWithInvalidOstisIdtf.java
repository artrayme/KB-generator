package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class WikiRemoveEntitiesWithInvalidOstisIdtf implements WikiProcessorPipeline {

    private final WikiProcessorPipeline wikiProcessorPipeline;

    public WikiRemoveEntitiesWithInvalidOstisIdtf(WikiProcessorPipeline wikiProcessorPipeline) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
    }

    @Override
    public WikiDataContainer execute() {
        var data = wikiProcessorPipeline.execute();
        //        just get wiki ids where mapped label contains invalid characters
        var allMappers = new HashMap<>(data.getConceptsWikiToOstisMap());
        allMappers.putAll(data.getInstancesWikiToOstisMap());
        allMappers.putAll(data.getPropertiesWikiToOstisMap());
        var wikiIdsToDelete = allMappers.entrySet().stream()
                .filter((k) -> isInvalidOstisIdtf(k.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        //        remove corrupted wiki ids from other maps
        data.deleteByWikiIds(wikiIdsToDelete);
        //        data.getConceptsWikiToOstisMap().keySet().removeIf(wikiIdsToDelete::contains);
        //        data.getInstancesWikiToOstisMap().keySet().removeIf(wikiIdsToDelete::contains);
        //        data.getPropertiesWikiToOstisMap().keySet().removeIf(wikiIdsToDelete::contains);
        //        data.getTriplets().removeIf(
        //                e -> wikiIdsToDelete.contains(e.node1())
        //                        || wikiIdsToDelete.contains(e.property())
        //                        || wikiIdsToDelete.contains(e.node2())
        //        );
        //        data.getAllData().removeIf(e -> wikiIdsToDelete.contains(e.wikiId()));

        return data;
    }

    private boolean isInvalidOstisIdtf(String idtf) {
        //        Ostis id can't include non-ASCII symbols and special symbols
        return (idtf.contains(":")
                || idtf.contains("'")
                || idtf.contains("-")
                || idtf.contains("/")
                || idtf.contains(".")
                || idtf.contains(",")
                || idtf.contains("(")
                || idtf.contains(")")
                || idtf.contains("[")
                || idtf.contains("]")
                || idtf.contains("|")
                || !StandardCharsets.US_ASCII.newEncoder().canEncode(idtf)
        );
    }
}
