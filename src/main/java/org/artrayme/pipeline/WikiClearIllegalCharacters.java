package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;

public class WikiClearIllegalCharacters implements WikiProcessorPipeline{

    private final WikiProcessorPipeline wikiProcessorPipeline;

    public WikiClearIllegalCharacters(WikiProcessorPipeline wikiProcessorPipeline) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
    }
    @Override
    public WikiDataContainer execute() {
        var container = wikiProcessorPipeline.execute();
        container.getAllData().stream().map(WikiEntity::descriptions).forEach(e->{
            e.replaceAll((k,v)->removeIllegalCharacters(v));
        });
        return container;
    }
    private String removeIllegalCharacters(String text){
        return text.replace("[","")
                .replace("]","");
    }
}
