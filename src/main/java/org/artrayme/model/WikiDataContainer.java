package org.artrayme.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WikiDataContainer {
    private final List<WikiEntity> allData = new ArrayList<>();
    private final Map<String, String> conceptsWikiToOstisMap = new HashMap<>();
    private final Map<String, String> propertiesWikiToOstisMap = new HashMap<>();
    private final Map<String, String> instancesWikiToOstisMap = new HashMap<>();

    private final Map<String, Set<String>> classInstancesMap = new HashMap<>();

    private final List<WikiTriplet> triplets = new ArrayList<>();


    public WikiDataContainer(List<WikiEntity> allData,
                             Map<String, String> conceptsWikiToOstisMap,
                             Map<String, String> propertiesWikiToOstisMap,
                             Map<String, String> instancesWikiToOstisMap,
                             Map<String, Set<String>> classInstancesMap,
                             List<WikiTriplet> triplets) {
        this.allData.addAll(allData);
        this.conceptsWikiToOstisMap.putAll(conceptsWikiToOstisMap);
        this.propertiesWikiToOstisMap.putAll(propertiesWikiToOstisMap);
        this.instancesWikiToOstisMap.putAll(instancesWikiToOstisMap);
        this.classInstancesMap.putAll(classInstancesMap);
        this.triplets.addAll(triplets);
    }

    public List<WikiEntity> getAllData() {
        return allData;
    }

    public Map<String, String> getConceptsWikiToOstisMap() {
        return conceptsWikiToOstisMap;
    }

    public Map<String, String> getPropertiesWikiToOstisMap() {
        return propertiesWikiToOstisMap;
    }

    public Map<String, String> getInstancesWikiToOstisMap() {
        return instancesWikiToOstisMap;
    }

    public List<WikiTriplet> getTriplets() {
        return triplets;
    }

    public Map<String, Set<String>> getClassInstancesMap() {
        return classInstancesMap;
    }

    public boolean addInstanceForClass(String classWikiId, Set<String> instancesToAdd){
        if (classInstancesMap.containsKey(classWikiId)){
            return classInstancesMap.get(classWikiId).addAll(instancesToAdd);
        } else classInstancesMap.put(classWikiId, new HashSet<>(instancesToAdd));
        return true;
    }

    public void deleteByWikiIds(Set<String> wikiIds) {
        conceptsWikiToOstisMap.keySet().removeIf(wikiIds::contains);
        instancesWikiToOstisMap.keySet().removeIf(wikiIds::contains);
        propertiesWikiToOstisMap.keySet().removeIf(wikiIds::contains);
        classInstancesMap.keySet().removeIf(wikiIds::contains);
        classInstancesMap.values().forEach(e->e.removeIf(wikiIds::contains));
        classInstancesMap.values().removeIf(Set::isEmpty);
        triplets.removeIf(
                e -> wikiIds.contains(e.node1())
                        || wikiIds.contains(e.property())
                        || wikiIds.contains(e.node2())
        );
        allData.removeIf(e -> wikiIds.contains(e.wikiId()));
    }
}
