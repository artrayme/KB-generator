package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WikiExtractInstancesFromConcepts implements WikiProcessorPipeline {
    private final WikiProcessorPipeline wikiProcessorPipeline;
    private final Set<String> instanceOfRelations;
    private final Set<String> hasInstanceRelations;
    private final Set<String> subclassRelation;

    public WikiExtractInstancesFromConcepts(WikiProcessorPipeline wikiProcessorPipeline,
                                            Set<String> instanceOfRelations,
                                            Set<String> hasInstanceRelations,
                                            Set<String> subclassRelation) {
        this.wikiProcessorPipeline = wikiProcessorPipeline;
        this.instanceOfRelations = instanceOfRelations;
        this.hasInstanceRelations = hasInstanceRelations;
        this.subclassRelation = subclassRelation;
    }

    @Override
    public WikiDataContainer execute() {
        var container = wikiProcessorPipeline.execute();
        var classes = container.getTriplets().stream()
                .filter(e -> subclassRelation.contains(e.property()))
                .flatMap(e -> Stream.of(e.node1(), e.node2()))
                .collect(Collectors.toSet());

        container.getTriplets().stream()
                .filter(e -> !container.getPropertiesWikiToOstisMap().containsKey(e.node1()))
                .filter(e -> hasInstanceRelations.contains(e.property()))
                .forEach(e -> {
                    classes.add(e.node1());
                    container.getInstancesWikiToOstisMap().put(e.node2(), null);
                    container.getConceptsWikiToOstisMap().remove(e.node2());
                    container.addInstanceForClass(e.node1(), Set.of(e.node2()));
                });

        container.getTriplets().stream()
                .filter(e -> instanceOfRelations.contains(e.property())
                        && !classes.contains(e.node1())
                        && !container.getPropertiesWikiToOstisMap().containsKey(e.node1())
                ).forEach(e -> {
                    container.getInstancesWikiToOstisMap().put(e.node1(), null);
                    container.getConceptsWikiToOstisMap().remove(e.node1());
                    container.addInstanceForClass(e.node2(), Set.of(e.node1()));
                });
        return container;
    }
}
