package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiTriplet;
import org.wikidata.wdtk.datamodel.implementation.ItemDocumentImpl;
import org.wikidata.wdtk.datamodel.implementation.PropertyDocumentImpl;
import org.wikidata.wdtk.datamodel.implementation.ValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.wikibaseapi.WbSearchEntitiesResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class WikiBatchEntityCollector implements WikiProcessorPipeline {

    private final Map<String, String> keywords = new HashMap<>();
    private final int recursionDepth;
    private final int pagesCount;

    private final List<WikiTriplet> triplets = new ArrayList<>();
    private final Map<String, String> conceptsMap = new HashMap<>();
    private final Map<String, String> propertiesMap = new HashMap<>();
    private final Map<String, String> instancesMap = new HashMap<>();

    private final WikibaseDataFetcher wbdf;

    public WikiBatchEntityCollector(Map<String, String> keywords, int depth, int firstNPages, Set<String> propertiesWhiteList, Set<String> requiredLangs, WikibaseDataFetcher wbdf) {
        this.wbdf = wbdf;
        this.keywords.putAll(keywords);
        recursionDepth = depth;
        pagesCount = firstNPages;

        this.wbdf.getFilter().setLanguageFilter(requiredLangs);
        try {
            var pulledProperties = this.wbdf.getEntityDocuments(propertiesWhiteList.stream().toList());
            this.wbdf.getFilter().setPropertyFilter(pulledProperties.values().stream().map(e -> ((PropertyDocumentImpl) e).getEntityId()).collect(Collectors.toSet()));
        } catch (MediaWikiApiErrorException | IOException e) {
            throw new RuntimeException("Can't find one of your properties from whitelist", e);
        }
    }

    @Override
    public WikiDataContainer execute() {
        var collectedDocuments = keywords.entrySet().stream()
                .map(e -> this.noExceptSearchEntities(e.getKey(), e.getValue()))
                .flatMap(e -> e.stream().limit(pagesCount))
                .toList();
        for (WbSearchEntitiesResult document : collectedDocuments) {
            recPullConnectedData(document.getEntityId(), recursionDepth);
        }

        return new WikiDataContainer(List.of(), conceptsMap, propertiesMap, instancesMap, Map.of(), triplets);
    }

    private void recPullConnectedData(String documentID, int recursionDepth) {
        if (recursionDepth == 0)
            return;
        Map<String, List<String>> connectedElements = new HashMap<>();
        if (documentID.startsWith("Q")) {
            var result = pullEntity(documentID);
            for (ItemDocumentImpl itemDocument : result) {
                var connectedEntities = getConnectedElements(itemDocument.getJsonClaims());
                connectedElements.putAll(connectedEntities);
            }
        } else if (documentID.startsWith("P")) {
            var result = pullProperty(documentID);
            for (PropertyDocumentImpl propertyDocument : result) {
                var connectedProperties = getConnectedElements(propertyDocument.getJsonClaims());
                connectedElements.putAll(connectedProperties);
            }
        } else {
            throw new RuntimeException("Parser can only work with Items(Q) and Properties(P), but not with " + documentID);
        }

        conceptsMap.put(documentID, null);
        connectedElements.forEach((k, v) -> {
            propertiesMap.put(k, null);
            for (String s : v) {
                triplets.add(new WikiTriplet(documentID, k, s));
                conceptsMap.put(s, null);
            }
        });

        connectedElements.values().stream().flatMap(Collection::stream).distinct().forEach(e -> {
            recPullConnectedData(e, recursionDepth - 1);
        });
        connectedElements.keySet().forEach(e -> {
            recPullConnectedData(e, recursionDepth - 1);
        });

    }

    private List<PropertyDocumentImpl> pullProperty(String id) {
        return noExceptGetEntityDocument(id).stream()
                .map(e -> (PropertyDocumentImpl) e)
                .toList();
    }

    private List<ItemDocumentImpl> pullEntity(String id) {
        return noExceptGetEntityDocument(id).stream()
                .map(e -> (ItemDocumentImpl) e)
                .toList();
    }

    private Map<String, List<String>> getConnectedElements(Map<String, List<Statement>> claims) {
        var result = claims.entrySet().stream()
                .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                g -> g.getValue()
                                        .stream()
                                        .map(Statement::getMainSnak)
                                        .filter(e -> e instanceof ValueSnakImpl)
                                        .map(e -> (ValueSnakImpl) e)
                                        .map(ValueSnakImpl::getDatavalue)
                                        .filter(e -> e instanceof ItemIdValue)
                                        .map(e -> (ItemIdValue) e)
                                        .map(EntityIdValue::getId)
                                        .toList()
                        )
                );
        result.values().removeIf(List::isEmpty);
        return result;
    }

    private List<WbSearchEntitiesResult> noExceptSearchEntities(String word, String language) {
        try {
            return wbdf.searchEntities(word, language);
        } catch (MediaWikiApiErrorException | IOException e) {
            throw new RuntimeException("Can't find " + word + " page. Please, choose another one and run again");
        }
    }

    private Optional<EntityDocument> noExceptGetEntityDocument(String wikidataID) {
        try {
            return Optional.ofNullable(wbdf.getEntityDocument(wikidataID));
        } catch (MediaWikiApiErrorException | IOException e) {
            throw new RuntimeException("Can't find " + wikidataID + " entity. Please, choose another one and run again");
        }
    }
}
