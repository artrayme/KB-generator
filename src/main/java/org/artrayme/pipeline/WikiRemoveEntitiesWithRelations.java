package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;
import org.artrayme.model.WikiTriplet;
import org.artrayme.translator.jafregle.Language;

import java.util.Set;
import java.util.stream.Collectors;

public class WikiRemoveEntitiesWithRelations implements WikiProcessorPipeline {
    private final WikiProcessorPipeline wikiProcessorPipeline;
    private final Set<String> relations;

    public WikiRemoveEntitiesWithRelations(WikiProcessorPipeline wikiProcessorPipeline, Set<String> relations) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
        this.relations = relations;
    }

    @Override
    public WikiDataContainer execute() {
        var data = wikiProcessorPipeline.execute();
        var wikiIdsToDelete = data.getTriplets().stream()
                .filter(e -> this.relations.contains(e.property()))
                .map(WikiTriplet::node2)
                .collect(Collectors.toSet());

        //        remove corrupted wiki ids from other maps
        data.deleteByWikiIds(wikiIdsToDelete);
        return data;
    }
}
