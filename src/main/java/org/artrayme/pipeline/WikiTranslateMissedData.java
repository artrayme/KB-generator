package org.artrayme.pipeline;

import org.artrayme.model.WikiDataContainer;
import org.artrayme.model.WikiEntity;
import org.artrayme.translator.jafregle.Jafregle;
import org.artrayme.translator.jafregle.Language;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class WikiTranslateMissedData implements WikiProcessorPipeline {
    private final Jafregle translator;
    private final WikiProcessorPipeline wikiProcessorPipeline;
    private  final Set<String> requiredLangs;
    private boolean isBannedByGoogle;

    public WikiTranslateMissedData(WikiProcessorPipeline wikiProcessorPipeline, Jafregle translator, Set<String> requiredLangs) {
        this.translator = translator;
        this.wikiProcessorPipeline = wikiProcessorPipeline;
        this.requiredLangs = requiredLangs;
    }

    @Override
    public WikiDataContainer execute() {
        return translateAllData(wikiProcessorPipeline.execute());
    }

    private WikiDataContainer translateAllData(WikiDataContainer container){
        for (WikiEntity entity : container.getAllData()) {
            translateWikiData(entity.labels());
            translateWikiData(entity.descriptions());
        }
        return container;
    }

    private void translateWikiData(Map<String, String> map) {
        //               Wiki articles in English are the best in general
        if (map.containsKey(Language.ENGLISH.value())) {
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

        }
    }
}
