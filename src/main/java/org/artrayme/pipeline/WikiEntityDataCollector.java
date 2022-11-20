package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;
import org.wikidata.wdtk.datamodel.implementation.TermedStatementDocumentImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WikiEntityDataCollector implements WikiProcessorPipeline {
    private final WikiProcessorPipeline wikiProcessorPipeline;
    private final WikibaseDataFetcher wbdf;


    public WikiEntityDataCollector(WikiProcessorPipeline wikiProcessorPipeline, WikibaseDataFetcher wbdf) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
        this.wbdf = wbdf;
    }

    @Override
    public WikiDataContainer execute() {
        var container = wikiProcessorPipeline.execute();
        var pulledData = pullDataForIds(Stream.of(container.getConceptsWikiToOstisMap().keySet().stream(),
                        container.getInstancesWikiToOstisMap().keySet().stream(),
                        container.getPropertiesWikiToOstisMap().keySet().stream())
                .flatMap(e -> e).toList()
        );
        return new WikiDataContainer(pulledData.values().stream().toList(),
                container.getConceptsWikiToOstisMap(),
                container.getPropertiesWikiToOstisMap(),
                container.getInstancesWikiToOstisMap(),
                container.getClassInstancesMap(),
                container.getTriplets()
        );
    }

    private Map<String, WikiEntity> pullDataForIds(List<String> wikiIds) {
        try {
            Map<String, WikiEntity> result = new HashMap<>();
            Set<Map.Entry<String, EntityDocument>> documents = wbdf.getEntityDocuments(wikiIds).entrySet();
            for (Map.Entry<String, EntityDocument> entityDocument : documents) {
                var labels = ((TermedStatementDocumentImpl) entityDocument.getValue()).getLabels().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().getText()));

                var descriptions = ((TermedStatementDocumentImpl) entityDocument.getValue()).getDescriptions().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().getText()));

                result.put(entityDocument.getKey(), new WikiEntity(
                        entityDocument.getKey(),
                        labels,
                        descriptions
                ));
            }
            return result;
        } catch (MediaWikiApiErrorException | IOException e) {
            //            Impossible situation because all collected wiki Ids must exist
            //            It can be called only in case of wiki API error
            throw new RuntimeException("Something wrong with Wiki API. Try again");
        }
    }
}
