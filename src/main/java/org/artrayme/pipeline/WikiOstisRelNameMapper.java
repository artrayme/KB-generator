package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;

import java.util.Map;

public class WikiOstisRelNameMapper implements WikiProcessorPipeline {
    private final WikiProcessorPipeline wikiProcessorPipeline;
    private final Map<String, String> relationMap;

    public WikiOstisRelNameMapper(WikiProcessorPipeline wikiProcessorPipeline, Map<String, String> relationMap) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
        this.relationMap = relationMap;
    }

    @Override
    public WikiDataContainer execute() {
        var container = wikiProcessorPipeline.execute();
        container.getPropertiesWikiToOstisMap().replaceAll(relationMap::getOrDefault);
        container.getAllData().removeIf(e -> relationMap.containsKey(e.wikiId()));
        return container;
    }
}
