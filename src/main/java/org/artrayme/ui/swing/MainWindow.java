package org.artrayme.ui.swing;

import org.artrayme.converter.scs.ConceptConverter;
import org.artrayme.converter.scs.InstanceConverter;
import org.artrayme.converter.scs.RelationConverter;
import org.artrayme.pipeline.WikiBatchEntityCollector;
import org.artrayme.pipeline.WikiClearIllegalCharacters;
import org.artrayme.pipeline.WikiDeleteEntitiesWithoutLabels;
import org.artrayme.pipeline.WikiEntityDataCollector;
import org.artrayme.pipeline.WikiExtractInstancesFromConcepts;
import org.artrayme.pipeline.WikiFillMappingInfo;
import org.artrayme.pipeline.WikiOstisRelNameMapper;
import org.artrayme.pipeline.WikiProcessorPipeline;
import org.artrayme.pipeline.WikiRemoveEntitiesWithInvalidOstisIdtf;
import org.artrayme.pipeline.WikiRemoveEntitiesWithRelations;
import org.artrayme.pipeline.WikiRemoveEntitiesWithoutEnglish;
import org.artrayme.pipeline.WikiTranslateMissedData;
import org.artrayme.translator.jafregle.Jafregle;
import org.artrayme.translator.jafregle.translators.FreeGoogleTranslator;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MainWindow extends JFrame {
    private final JTextField entityNames = new JTextField("human, planet, economy, economist, Q9381");
    private final JTextField requiredLangs = new JTextField("ru, en, de, fr, uk");
    private final JButton generateButton = new JButton("Generate");
    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final JPanel fieldsPanel = new JPanel(new GridLayout(2, 2));

    public MainWindow() throws HeadlessException {

        Font mainFont = new Font("SansSerif", Font.BOLD, 20);
        entityNames.setFont(mainFont);
        entityNames.getText();
        requiredLangs.setFont(mainFont);
        requiredLangs.getText();

        fieldsPanel.add(new JLabel("Entities:"));
        fieldsPanel.add(entityNames);

        fieldsPanel.add(new JLabel("Languages:"));
        fieldsPanel.add(requiredLangs);

        mainPanel.add(fieldsPanel, BorderLayout.CENTER);
        mainPanel.add(generateButton, BorderLayout.SOUTH);

        generateButton.addActionListener(e -> {
            try {
                generate();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(new JFrame(), "Something wrong with files", "Dialog",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        this.add(mainPanel);
    }

    public static void main(String[] args) throws IOException {

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        MainWindow window = new MainWindow();

        window.setBounds(400, 400, 900, 200);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    private void generate() throws IOException {
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
                "P5869",  // model_item
                "P279",   // subclass_of
                "P101",   // field_of_work
                "P1855",   // Wiki_property_example
                "P106"    // occupation
        );
        Set<String> relationsInstanceOf = Set.of("P31", "P106", "P1855");
        Set<String> relationsHasInstance = Set.of("P5869");
        Set<String> relationsSubclassOf = Set.of("P279");

        var requiredLangs = Arrays.stream(this.requiredLangs.getText().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        var requiredEntities = new HashMap<String, String>();
        Arrays.stream(this.entityNames.getText().split(","))
                .map(String::trim).forEach(e -> {
                    requiredEntities.put(e, "en");
                });

        WikibaseDataFetcher wikidataDataFetcher = WikibaseDataFetcher.getWikidataDataFetcher();
        WikiProcessorPipeline wikiParser = new WikiBatchEntityCollector(
                requiredEntities,
                1,
                1,
                allowableProperties,
                requiredLangs,
                wikidataDataFetcher
        );

        var pipeline = new WikiClearIllegalCharacters(
                new WikiOstisRelNameMapper(
                        new WikiRemoveEntitiesWithInvalidOstisIdtf(
                                new WikiFillMappingInfo(
                                        new WikiRemoveEntitiesWithoutEnglish(
                                                new WikiTranslateMissedData(
                                                        new WikiRemoveEntitiesWithRelations(
                                                                new WikiDeleteEntitiesWithoutLabels(
                                                                        new WikiEntityDataCollector(
                                                                                new WikiExtractInstancesFromConcepts(
                                                                                        wikiParser,
                                                                                        relationsInstanceOf,
                                                                                        relationsHasInstance,
                                                                                        relationsSubclassOf),
                                                                                wikidataDataFetcher
                                                                        )
                                                                ),
                                                                Set.of("P2959")
                                                        ),
                                                        new Jafregle(new FreeGoogleTranslator()),
                                                        requiredLangs
                                                )
                                        )
                                )
                        ),
                        Map.of("P527", "nrel_basic_decomposition",
                                "P1552", "nrel_inclusion")
                )
        );
        var dataContainer = pipeline.execute();
        var conceptScs = new ConceptConverter(dataContainer, relationsSubclassOf).convert();
        var instancesScs = new InstanceConverter(dataContainer, relationsInstanceOf).convert();
        var relationsScs = new RelationConverter(dataContainer).convert();

        Path outDir = Path.of("sc_out");
        Path concOutDir = Path.of("sc_out/concepts");
        Path instOutDir = Path.of("sc_out/concepts/instances");
        Path relOutDir = Path.of("sc_out/relations");
        if (Files.exists(outDir)) {
            Files.walk(outDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectory(outDir);
        Files.createDirectory(concOutDir);
        Files.createDirectory(instOutDir);
        Files.createDirectory(relOutDir);

        conceptScs.forEach((k, v) -> {
            try (PrintWriter out = new PrintWriter(concOutDir + "/" + k + ".scs")) {
                out.println(v);
            } catch (FileNotFoundException ignored) {
            }
        });

        instancesScs.forEach((k, v) -> {
            try (PrintWriter out = new PrintWriter(instOutDir + "/" + k + ".scs")) {
                out.println(v);
            } catch (FileNotFoundException ignored) {
            }
        });

        relationsScs.forEach((k, v) -> {
            try (PrintWriter out = new PrintWriter(relOutDir + "/" + k + ".scs")) {
                out.println(v);
            } catch (FileNotFoundException ignored) {
            }
        });

        JOptionPane.showMessageDialog(new JFrame(), "Complete. Check sc_out folder", "Dialog",
                JOptionPane.INFORMATION_MESSAGE);
    }

}
