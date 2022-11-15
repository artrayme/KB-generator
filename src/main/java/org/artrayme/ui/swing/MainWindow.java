package org.artrayme.ui.swing;

import org.artrayme.converter.scs.ConceptConverter;
import org.artrayme.converter.scs.InstanceConverter;
import org.artrayme.converter.scs.RelationConverter;
import org.artrayme.pipeline.WikiBatchEntityCollector;
import org.artrayme.pipeline.WikiClearIllegalCharacters;
import org.artrayme.pipeline.WikiEntityDataCollector;
import org.artrayme.pipeline.WikiFillMappingInfo;
import org.artrayme.pipeline.WikiProcessorPipeline;
import org.artrayme.pipeline.WikiRemoveEntitiesWithInvalidOstisIdtf;
import org.artrayme.pipeline.WikiRemoveEntitiesWithoutEnglish;
import org.artrayme.pipeline.WikiTranslateMissedData;
import org.artrayme.translator.jafregle.Jafregle;
import org.artrayme.translator.jafregle.translators.FreeGoogleTranslator;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class MainWindow {

    public static void main(String[] args) throws IOException {
        var allowableProperties = Set.of("P31",
                "P1552",  // has_quality
                "P361",   // part_of
                "P527",   // has_parts
                "P2283",  // uses
                "P111",   // measure_physical_quantity
                "P1889",  // different_from
                "P1269",  // facet_of
                "P1542",  // has_effect
                "P1557",  // manifestation_of
                "P461",   // opposite_of
                "P1382",  // partially_coincident_with
                "P1344",  // participant_in
                "P129",   // physically_interacts_with
                "P1056",  // product_or_material_produced
                "P3342",  // significant_person
                "P2579",  // studied_by
                "P1709",  // equivalent_class
                "P495",   // country_of_origin
                "P425",   // field_of_this_occupation
                "P5869",   // model_item
                "P279"   // subclass_of
        );

        var requiredLangs = Set.of("en", "ru", "de", "fr", "uk");
        WikibaseDataFetcher wikidataDataFetcher = WikibaseDataFetcher.getWikidataDataFetcher();
        WikiProcessorPipeline wikiParser = new WikiBatchEntityCollector(
                Map.of("human", "en"),
                1,
                1,
                allowableProperties,
                requiredLangs,
                wikidataDataFetcher
        );

        var pipeline = new WikiClearIllegalCharacters(
                new WikiRemoveEntitiesWithInvalidOstisIdtf(
                        new WikiFillMappingInfo(
                                new WikiRemoveEntitiesWithoutEnglish(
                                        new WikiTranslateMissedData(
                                                new WikiEntityDataCollector(
                                                        wikiParser,
                                                        wikidataDataFetcher
                                                ),
                                                new Jafregle(new FreeGoogleTranslator()),
                                                requiredLangs
                                        )
                                )
                        )
                )
        );
        var x = pipeline.execute();
        var conceptScs = new ConceptConverter(x).convert();
        var instancesScs = new InstanceConverter(x).convert();
        var relationsScs = new RelationConverter(x).convert();

        Path outDir = Path.of("sc_out");
        Path concOutDir = Path.of("sc_out/concepts");
        Path instOutDir = Path.of("sc_out/concepts/instances");
        Path relOutDir = Path.of("sc_out/relations");
        if(Files.exists(outDir)){
            Files.walk(outDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectory(outDir);
        Files.createDirectory(concOutDir);
        Files.createDirectory(instOutDir);
        Files.createDirectory(relOutDir);

        conceptScs.forEach((k,v)->{
            try (PrintWriter out = new PrintWriter(concOutDir+"/" + k + ".scs")) {
                out.println(v);
            } catch (FileNotFoundException ignored) {}
        });

        instancesScs.forEach((k,v)->{
            try (PrintWriter out = new PrintWriter(instOutDir+"/" + k + ".scs")) {
                out.println(v);
            } catch (FileNotFoundException ignored) {}
        });

        relationsScs.forEach((k,v)->{
            try (PrintWriter out = new PrintWriter(relOutDir+"/" + k + ".scs")) {
                out.println(v);
            } catch (FileNotFoundException ignored) {}
        });


    }

}
