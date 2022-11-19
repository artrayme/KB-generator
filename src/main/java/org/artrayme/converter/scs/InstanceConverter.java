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

public class InstanceConverter implements WikiDataContainerToScsConverter {
    private final WikiDataContainer container;
    private final String stTemplate;
    private ST parser;

    public InstanceConverter(WikiDataContainer container) throws IOException {
        this.container = container;
        URL file = InstanceConverter.class.getResource("/ScsTemplates/instance.st");
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
        var data = container.getAllData().stream().collect(Collectors.toMap(WikiEntity::wikiId, e -> e));
        container.getInstancesWikiToOstisMap().forEach((key, value) -> {
            if (data.containsKey(key)) {
                result.put(value, convertSingle(key, value, data));
            }
        });

        return result;
    }

    private String convertSingle(String key, String value, Map<String, WikiEntity> data) {
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

        data.get(key).descriptions().forEach((lang, label) -> {
            parser.addAggr("examples.{text, lang}", List.of(label, "lang_" + lang).toArray());
        });

        parser.add("class", container.getConceptsWikiToOstisMap().get(container.getTriplets().stream().filter(e -> e.node2().equals(key)).findFirst().get().node1()));

        return parser.render();
    }
}
