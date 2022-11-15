package org.artrayme;

import org.artrayme.translator.jafregle.http.HttpClient;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
//
        BatchParser parser = new BatchParser(
                Set.of("P31",
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
                ),
                Set.of("en", "ru", "de", "fr", "uk")
        );
        parser.parse(List.of(
                new AbstractMap.SimpleEntry<>("human", "en"),
                new AbstractMap.SimpleEntry<>("gender", "en"),
                new AbstractMap.SimpleEntry<>("planet", "en"),
                new AbstractMap.SimpleEntry<>("Earth", "en")
                ),
                1, 1);
        System.out.printf("");

//        URL file = Main.class.getResource("/ScsTemplates/concept.st");
//        ST st = new ST(Files.readString(Path.of(file.getFile())), '$', '$');
//        st.add("idtf", "my_first_concept");
//        st.addAggr("labels.{text, lang}", List.of("QWE", "xxx").toArray());
////        st.addAggr("labels.{text, lang}", List.of("AAAAA", "f").toArray());
////
////        st.addAggr("definitions.{text, lang}", List.of("m", "f").toArray());
////        st.addAggr("definitions.{text, lang}", List.of("k", "f").toArray());
////
////        st.addAggr("definitionConcepts.{idtf}", List.of( "conc1").toArray());
////        st.addAggr("definitionConcepts.{idtf}", List.of( "conc222").toArray());
////
////        st.addAggr("examples.{text, lang}", List.of("a", "f").toArray());
////        st.addAggr("examples.{text, lang}", List.of("b", "f").toArray());
////        st.addAggr("examples.{text, lang}", List.of("c", "f").toArray());
//
//        System.out.println(st.render());
//        try (PrintWriter out = new PrintWriter("test.scs")) {
//            out.println(st.render());
//        }
    }

}