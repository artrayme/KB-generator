$idtf$

//идентификаторы
=> nrel_main_idtf:
    $labels:{label|[$label.text$] (*<-$label.lang$;;*)}; separator=";\n"$;

$if(definitions)$
//определение
<- rrel_key_sc_element: ... (*
	<- sc_definition;;
    => nrel_main_idtf:
       $definitions:{def|[$def.text$] (*<-$def.lang$;;*)}; separator=";\n"$;;
    $if(definitionConcepts)$
    => nrel_using_constants: {
        $definitionConcepts:{conc|$conc.idtf$}; separator=";\n"$
    };;
    $endif$
    $if(examples)$
    <= nrel_sc_text_translation: ... (*
          -> rrel_example:
                $examples:{example|[$example.text$] (*<-$example.lang$;; => nrel_format: format_html;;*)}; separator=";\n"$;;
    *);;
    $endif$
*);
$endif$

$if(firstDomains)$
$firstDomains:{conc|
=> nrel_first_domain:
    $conc.property$;
}; separator="\n"$
$endif$

$if(secondDomains)$
$secondDomains:{conc|
=> nrel_second_domain:
    $conc.property$;
}; separator="\n"$
$endif$

<- sc_node_norole_relation;
<- relation;
<- binary_relation;
<- oriented_relation;
<- antireflexive_relation;
<- asymmetric_relation;
<- antitransitive_relation;;