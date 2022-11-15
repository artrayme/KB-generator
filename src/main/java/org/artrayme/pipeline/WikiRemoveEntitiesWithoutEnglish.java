package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;
import org.artrayme.translator.jafregle.Language;

import java.util.stream.Collectors;

public class WikiRemoveEntitiesWithoutEnglish implements WikiProcessorPipeline{
    private final WikiProcessorPipeline wikiProcessorPipeline;

    public WikiRemoveEntitiesWithoutEnglish(WikiProcessorPipeline wikiProcessorPipeline) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
    }

    @Override
    public WikiDataContainer execute() {
        var data = wikiProcessorPipeline.execute();
//        just get wiki ids of items where labels don't have label in English
        var entitiesToDelete = data.getAllData().stream()
                .filter(e->!e.labels().containsKey(Language.ENGLISH.value()))
                .map(WikiEntity::wikiId)
                .collect(Collectors.toSet());

//        remove corrupted wiki ids from other maps
        data.getConceptsWikiToOstisMap().keySet().removeIf(entitiesToDelete::contains);
        data.getInstancesWikiToOstisMap().keySet().removeIf(entitiesToDelete::contains);
        data.getPropertiesWikiToOstisMap().keySet().removeIf(entitiesToDelete::contains);
        data.getTriplets().removeIf(
                e->entitiesToDelete.contains(e.node1())
                ||entitiesToDelete.contains(e.property())
                ||entitiesToDelete.contains(e.node2())
        );
        data.getAllData().removeIf(e->entitiesToDelete.contains(e.wikiId()));

        return data;
    }
}
