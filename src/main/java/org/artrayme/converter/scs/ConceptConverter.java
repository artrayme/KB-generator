package org.artrayme.converter.scs;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;
import org.artrayme.translator.jafregle.Language;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConceptConverter implements WikiDataContainerToScsConverter {
    private final WikiDataContainer container;
    private ST parser;
    private final String stTemplate;

    public ConceptConverter(WikiDataContainer container) throws IOException {
        this.container = container;
        URL file = ConceptConverter.class.getResource("/ScsTemplates/concept.st");
        if (file != null) {
             stTemplate = Files.readString(Path.of(file.getFile()));
            parser = new ST(stTemplate, '$', '$');
        } else {
            throw new IOException("File with template not found");
        }
    }

    @Override
    public Map<String, String> convert() {
        Map<String, String> result = new HashMap<>();
        var data = container.getAllData().stream().collect(Collectors.toMap(WikiEntity::wikiId, e->e));
        container.getConceptsWikiToOstisMap().forEach((key, value) -> {
            result.put(value, convertSingle(key, value, data));
        });

        return result;
    }

    private String convertSingle(String key, String value, Map<String, WikiEntity> data){
//        I don't know how to reset state for StringTemplate
        parser = new ST(stTemplate, '$', '$');
        parser.add("idtf", value);

        data.get(key).labels().forEach((lang, label) -> {
            parser.addAggr("labels.{text, lang}", List.of(label, "lang_" + lang).toArray());
        });
        data.get(key).labels().forEach((lang, label) -> {
            if (lang.equals(Language.RUSSIAN.value())) {
                parser.addAggr("definitions.{text, lang}", List.of("Опр. (" + label + ")", "lang_" + lang).toArray());
            } else {
                parser.addAggr("definitions.{text, lang}", List.of("Def. (" + label + ")", "lang_" + lang).toArray());
            }
        });

        container.getTriplets().stream().filter(e -> e.node1().equals(key)).forEach(e -> {
            if (container.getPropertiesWikiToOstisMap().containsKey(e.property()) && container.getConceptsWikiToOstisMap().containsKey(e.node2()))
                parser.addAggr("relations.{text, conc}", List.of(container.getPropertiesWikiToOstisMap().get(e.property()),
                        container.getConceptsWikiToOstisMap().get(e.node2())).toArray());
        });

        data.get(key).descriptions().forEach((lang, label) -> {
            parser.addAggr("examples.{text, lang}", List.of(label, "lang_" + lang).toArray());
        });

        return parser.render();
    }
}
