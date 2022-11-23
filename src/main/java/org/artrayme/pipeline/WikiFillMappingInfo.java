package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;
import org.artrayme.translator.jafregle.Language;

import java.util.stream.Collectors;

public class WikiFillMappingInfo implements WikiProcessorPipeline {
    private final WikiProcessorPipeline wikiProcessorPipeline;

    public WikiFillMappingInfo(WikiProcessorPipeline wikiProcessorPipeline) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
    }

    @Override
    public WikiDataContainer execute() {
        var container = wikiProcessorPipeline.execute();
        var labelsMap = container.getAllData().stream().collect(Collectors.toMap(WikiEntity::wikiId, WikiEntity::labels));
        container.getPropertiesWikiToOstisMap().replaceAll((k, v) -> toOstisIdtf(labelsMap.get(k).get(Language.ENGLISH.value()), "nrel_"));
        container.getConceptsWikiToOstisMap().replaceAll((k, v) -> {
            String concept_ = toOstisIdtf(labelsMap.get(k).get(Language.ENGLISH.value()), "concept_");
            return concept_;
        });
        container.getInstancesWikiToOstisMap().replaceAll((k, v) -> toOstisIdtf(labelsMap.get(k).get(Language.ENGLISH.value()), ""));
        return container;
    }

    public String toOstisIdtf(String label, String prefix) {
        return prefix + label.replace(' ', '_')
                .replace("-", "_")
                .replace("+", "_")
                .replace(".", "_")
                .replace(",", "_")
                .replace("&", "_")
                .replace("|", "_")
                .replace("@", "")
                .replace("__", "_") //needs improvement
                .replace("___", "_") //needs improvement
                .replace("\"", "")
                .replace("'", "")
                .replace("(", "")
                .replace(")", "");
    }

}
