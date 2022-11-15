package org.artrayme;

import org.apache.commons.lang3.StringUtils;
import org.artrayme.translator.jafregle.Jafregle;
import org.artrayme.translator.jafregle.Language;
import org.artrayme.translator.jafregle.translators.FreeGoogleTranslator;
import org.stringtemplate.v4.ST;
import org.wikidata.wdtk.datamodel.implementation.ItemDocumentImpl;
import org.wikidata.wdtk.datamodel.implementation.MonolingualTextValueImpl;
import org.wikidata.wdtk.datamodel.implementation.PropertyDocumentImpl;
import org.wikidata.wdtk.datamodel.implementation.TermedStatementDocumentImpl;
import org.wikidata.wdtk.datamodel.implementation.ValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.wikibaseapi.WbSearchEntitiesResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BatchParser {
    private final List<WikiTriplet> triplets = new ArrayList<>();
    private final Map<String, String> conceptsMap = new HashMap<>();
    private final Map<String, String> propertiesMap = new HashMap<>();
    private final Map<String, String> instancesMap = new HashMap<>();

    private final Jafregle translator = new Jafregle(new FreeGoogleTranslator());
    private final WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();
    private final List<String> keywordsWithError = new ArrayList<>();
    private final List<String> idsWithError = new ArrayList<>();

    private boolean isBannedByGoogle = false;

    //    Translate translate = TranslateOptions.getDefaultInstance().getService();
    private final Set<String> requiredLangs;

    public BatchParser(Set<String> propertiesWhiteList, Set<String> requiredLangs) {
        wbdf.getFilter().setLanguageFilter(requiredLangs);
        try {
            var pulledProperties = wbdf.getEntityDocuments(propertiesWhiteList.stream().toList());
            wbdf.getFilter().setPropertyFilter(pulledProperties.values().stream().map(e -> ((PropertyDocumentImpl) e).getEntityId()).collect(Collectors.toSet()));
        } catch (MediaWikiApiErrorException | IOException e) {
            throw new RuntimeException("Can't find one of your properties from whitelist");
        }
        this.requiredLangs = requiredLangs;
    }

    public void parse(List<Map.Entry<String, String>> keywords, int depth, int firstNPages) {
        var collectedDocuments = keywords.stream()
                .map(e -> this.noExceptSearchEntities(e.getKey(), e.getValue()))
                .flatMap(e->e.stream().limit(firstNPages))
                .toList();

        for (WbSearchEntitiesResult document : collectedDocuments) {
            recPullConnectedData(document.getEntityId(), depth);
        }

        var pulledData = pullDataForIds(Stream.of(conceptsMap.keySet().stream(),
                        instancesMap.keySet().stream(),
                        propertiesMap.keySet().stream())
                .flatMap(e -> e).toList()
        );

        //        fillMissedData(pulledData);
        //        deleteIllegalCharacters(pulledData);

        deleteIllegalCharacters(pulledData);

        convertToOstisIdtfs(pulledData, conceptsMap, "concept_");
        convertToOstisIdtfs(pulledData, instancesMap, "");
        convertToOstisIdtfs(pulledData, propertiesMap, "nrel_");

        testGenFile(pulledData, conceptsMap, instancesMap, propertiesMap, triplets);

    }

    private void deleteIllegalCharacters(Map<String, WikiEntity> data) {
        data.values().stream().forEach(e->{
            e.description().replaceAll((k, v)->v.replace("[","").replace("]",""));
        });
//        data.forEach((k, entity) -> {
//            entity.description().forEach((k2, v) -> {
//                entity.description().put(k2, v.replace("[", "(")
//                        .replace("]", ")"));
//            });
//        });
//        data.values().stream().flatMap(e -> e.description().values().stream()).map(e -> e.replace("[", ""));
    }

    private void testGenFile(Map<String, WikiEntity> data, Map<String, String> mapper1, Map<String, String> mapper2, Map<String, String> mapper3, List<WikiTriplet> triplets) {
        try {
            Files.delete(Path.of("sc_out"));
        } catch (IOException e) {

        }
        try {
            Files.createDirectory(Path.of("sc_out"));
        } catch (IOException e) {
            //            throw new RuntimeException(e);
        }
        mapper1.forEach((k, v) -> {
            URL file = BatchParser.class.getResource("/ScsTemplates/concept.st");
            ST st = null;
            try {
                st = new ST(Files.readString(Path.of(file.getFile())), '$', '$');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String idtf = v;
            st.add("idtf", idtf);

            ST finalSt = st;
            data.get(k).labels().forEach((lang, label) -> {
                finalSt.addAggr("labels.{text, lang}", List.of(label, "lang_"+lang).toArray());
            });
            data.get(k).labels().forEach((lang, label) -> {
                if (lang.equals(Language.RUSSIAN.value())){
                    finalSt.addAggr("definitions.{text, lang}", List.of("Опр. (" + label + ")", "lang_"+lang).toArray());
                } else{
                    finalSt.addAggr("definitions.{text, lang}", List.of("Def. (" + label + ")", "lang_"+lang).toArray());
                }
            });

            triplets.stream().filter(e->e.node1().equals(k)).forEach(e->{
                if (propertiesMap.containsKey(e.property()) && conceptsMap.containsKey(e.node2()))
                    finalSt.addAggr("relations.{text, conc}", List.of(mapper3.get(e.property()), conceptsMap.get(e.node2())).toArray());
            });

//            triplets.stream().filter(e->e.node1().equals(k)).forEach(e->{
//                finalSt.addAggr("relations.{text, conc}", List.of(e.property(), e.node2()).toArray());
//            });
            data.get(k).description().forEach((lang, label) -> {
                    finalSt.addAggr("examples.{text, lang}", List.of(label, "lang_"+lang).toArray());
            });

            //        st.addAggr("definitionConcepts.{idtf}", List.of( "conc1").toArray());
            //        st.addAggr("definitionConcepts.{idtf}", List.of( "conc222").toArray());
            //
            //        st.addAggr("examples.{text, lang}", List.of("a", "f").toArray());
            //        st.addAggr("examples.{text, lang}", List.of("b", "f").toArray());
            //        st.addAggr("examples.{text, lang}", List.of("c", "f").toArray());

            try (PrintWriter out = new PrintWriter("sc_out/" + idtf + ".scs")) {
                out.println(st.render());
            } catch (FileNotFoundException e) {
                //                throw new RuntimeException(e);
            }
        });

        mapper2.forEach((k, v) -> {
            URL file = BatchParser.class.getResource("/ScsTemplates/instance.st");
            ST st = null;
            try {
                st = new ST(Files.readString(Path.of(file.getFile())), '$', '$');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String idtf = v;
            st.add("idtf", idtf);

            ST finalSt = st;
            data.get(k).labels().forEach((lang, label) -> {
                finalSt.addAggr("labels.{text, lang}", List.of(label, "lang_"+lang).toArray());
            });
            data.get(k).labels().forEach((lang, label) -> {
                if (lang.equals(Language.RUSSIAN.value())){
                    finalSt.addAggr("definitions.{text, lang}", List.of("Опр. (" + label + ")", "lang_"+lang).toArray());
                } else{
                    finalSt.addAggr("definitions.{text, lang}", List.of("Def. (" + label + ")", "lang_"+lang).toArray());
                }
            });
            finalSt.add("class", conceptsMap.get(triplets.stream().filter(e->e.node2().equals(k)).findFirst().get().node1()));


            data.get(k).description().forEach((lang, label) -> {
                finalSt.addAggr("examples.{text, lang}", List.of(label, "lang_"+lang).toArray());
            });
            //        st.addAggr("definitionConcepts.{idtf}", List.of( "conc1").toArray());
            //        st.addAggr("definitionConcepts.{idtf}", List.of( "conc222").toArray());
            //
            //        st.addAggr("examples.{text, lang}", List.of("a", "f").toArray());
            //        st.addAggr("examples.{text, lang}", List.of("b", "f").toArray());
            //        st.addAggr("examples.{text, lang}", List.of("c", "f").toArray());

            try (PrintWriter out = new PrintWriter("sc_out/" + idtf + ".scs")) {
                out.println(st.render());
            } catch (FileNotFoundException e) {
                //                throw new RuntimeException(e);
            }
        });

        mapper3.forEach((k, v) -> {
            URL file = BatchParser.class.getResource("/ScsTemplates/nrel.st");
            ST st;
            try {
                st = new ST(Files.readString(Path.of(file.getFile())), '$', '$');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String idtf = v;
            st.add("idtf", idtf);

            ST finalSt = st;

            var domains = triplets.stream().filter(e -> e.property().equals(k)).toList();
            var bigMapper = new HashMap<String, String>();
            bigMapper.putAll(mapper1);
            bigMapper.putAll(mapper2);
            bigMapper.putAll(mapper3);
            domains.stream().map(WikiTriplet::node1).distinct().forEach(e -> {
                if (bigMapper.containsKey(e)) {
                    String property = bigMapper.get(e);
                    finalSt.addAggr("firstDomains.{property}", List.of(property).toArray());
                }
            });
            domains.stream().map(WikiTriplet::node2).distinct().forEach(e -> {
                if (bigMapper.containsKey(e)) {
                    String property = bigMapper.get(e);
                    finalSt.addAggr("secondDomains.{property}", List.of(property).toArray());
                }
            });
            //            var secondDomains = domains.map(WikiTriplet::node2).toList();
            data.get(k).labels().forEach((lang, label) -> {
                finalSt.addAggr("labels.{text, lang}", List.of(label+"*", "lang_"+lang).toArray());
            });
            data.get(k).labels().forEach((lang, label) -> {
                if (lang.equals(Language.RUSSIAN.value())){
                    finalSt.addAggr("definitions.{text, lang}", List.of("Опр. (" + label + ")", "lang_"+lang).toArray());
                } else{
                    finalSt.addAggr("definitions.{text, lang}", List.of("Def. (" + label + ")", "lang_"+lang).toArray());
                }
            });

            data.get(k).description().forEach((lang, label) -> {
                finalSt.addAggr("examples.{text, lang}", List.of(label, "lang_"+lang).toArray());
            });
            //        st.addAggr("definitionConcepts.{idtf}", List.of( "conc1").toArray());
            //        st.addAggr("definitionConcepts.{idtf}", List.of( "conc222").toArray());
            //
            //        st.addAggr("examples.{text, lang}", List.of("a", "f").toArray());
            //        st.addAggr("examples.{text, lang}", List.of("b", "f").toArray());
            //        st.addAggr("examples.{text, lang}", List.of("c", "f").toArray());

            try (PrintWriter out = new PrintWriter("sc_out/" + idtf + ".scs")) {
                out.println(st.render());
            } catch (FileNotFoundException e) {
                //                throw new RuntimeException(e);
            }
        });

    }

    private void fillMissedData(Map<String, String> pulledData) {
        if (pulledData.size() < requiredLangs.size()) {
            translateWikiData(pulledData);
        }
    }

    private void translateWikiData(Map<String, String> map) {
        //               Wiki articles in English are the best in general
        if (map.containsKey("en")) {
            for (String requiredLang : requiredLangs) {
                if (!map.containsKey(requiredLang)) {
                    translate(map, Language.ENGLISH, Language.fromString(requiredLang));
                }
            }
            //                    But if there is no English articles -- try to find any language and use it as base
        } else {
            String lang = map.keySet().stream()
                    .findFirst().get();
            //                    .orElseThrow(() -> new RuntimeException("Can't find any label for entity" + idtf));
            for (String requiredLang : requiredLangs) {
                if (!map.containsKey(requiredLang)) {
                    translate(map, Language.fromString(lang), Language.fromString(requiredLang));
                }
            }
        }
    }

    private void translate(Map<String, String> text, Language from, Language to) {
        if (isBannedByGoogle)
            return;
        try {
            var translation = translator.translate(
                    text.get(from.value()),
                    from,
                    to);
            text.put(to.value(), translation);
            Thread.sleep(20);
        } catch (IOException | InterruptedException ex) {
            isBannedByGoogle = true;
            //                                If you have translated a lot before, Google will block your ip for "strange activity".
            //                                So skip the translation process, all data remains as before
            //            ToDo list with exceptions o smth like this
            //            System.out.print("error");

        }
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
                if (labels.isEmpty())
                    continue;
                else
                    fillMissedData(labels);

                var descriptions = ((TermedStatementDocumentImpl) entityDocument.getValue()).getDescriptions().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().getText()));
                if (descriptions.isEmpty())
                    continue;
                else
                    fillMissedData(descriptions);

                if (labels.containsKey("en")) {
                    result.put(entityDocument.getKey(), new WikiEntity(
                            entityDocument.getKey(),
                            labels,
                            descriptions
                    ));
                }
            }
            return result;
        } catch (MediaWikiApiErrorException | IOException e) {
            //            Impossible situation because all collected wiki Ids must exist
            //            It can be called only if api error
            throw new RuntimeException("Something wrong with Wiki API. Try again");
        }

    }

    private void convertToOstisIdtfs(Map<String, WikiEntity> pulledData, Map<String, String> wikiToOstisMap, String prefix) {
        wikiToOstisMap.keySet().stream().filter(e -> !pulledData.containsKey(e)).forEach(e -> {
            triplets.removeIf(k -> k.node1().equals(e) || k.node2().equals(e));
        });
        wikiToOstisMap.keySet().removeIf(e -> !pulledData.containsKey(e));
        wikiToOstisMap.replaceAll(
                (i, v) -> pulledData.get(i).labels().get("en")
                        .replace(' ', '_')
                        .replace("(", "")
                        .replace(")", "")
        );
        wikiToOstisMap.values().removeIf(e -> !isValidOstisIdtf(e));
        wikiToOstisMap.replaceAll((k, v) -> prefix + v);
    }

    private boolean isValidOstisIdtf(String idtf) {
        return !(idtf.contains(":")
                || idtf.contains("'")
                || idtf.contains("-")
                || idtf.contains("/")
                || !StandardCharsets.US_ASCII.newEncoder().canEncode(idtf)
        );
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

    private void recPullConnectedData(String documentID, int depth) {
        if (depth == 0)
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
            for (String s : v) {
                triplets.add(new WikiTriplet(documentID, k, s));
//                if (k.equals("P31")) {
//                    instancesMap.put(documentID, null);
//                    conceptsMap.put(s, null);
//                }
//                else
                if (k.equals("P5869")){
                    instancesMap.put(s, null);
                } else{
                    conceptsMap.put(s, null);
                }
            }
            propertiesMap.put(k, null);
        });

        connectedElements.values().stream().flatMap(Collection::stream).distinct().forEach(e -> {
            recPullConnectedData(e, depth - 1);
        });
        connectedElements.keySet().forEach(e -> {
            recPullConnectedData(e, depth - 1);
        });

    }

    private List<WbSearchEntitiesResult> noExceptSearchEntities(String word, String language) {
        try {
            return wbdf.searchEntities(word, language);
        } catch (MediaWikiApiErrorException | IOException e) {
            keywordsWithError.add(word);
            return List.of();
        }
    }

    private Optional<EntityDocument> noExceptGetEntityDocument(String wikidataID) {
        try {
            return Optional.ofNullable(wbdf.getEntityDocument(wikidataID));
        } catch (MediaWikiApiErrorException | IOException e) {
            idsWithError.add(wikidataID);
            return Optional.empty();
        }
    }
}
