package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;

import java.util.stream.Collectors;

public class WikiDeleteEntitiesWithoutLabels implements WikiProcessorPipeline{
    private final WikiProcessorPipeline wikiProcessorPipeline;

    public WikiDeleteEntitiesWithoutLabels(WikiProcessorPipeline wikiProcessorPipeline) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
    }

    @Override
    public WikiDataContainer execute() {
        var container = wikiProcessorPipeline.execute();
        var wikiIdsToDelete = container.getAllData().stream()
                .filter(e->e.labels().isEmpty())
                .map(WikiEntity::wikiId)
                .collect(Collectors.toSet());
        container.deleteByWikiIds(wikiIdsToDelete);
        return container;
    }
}
