$idtf$
=> nrel_main_idtf:
    $labels:{label|[$label.text$] (*<-$label.lang$;;*)}; separator=";\n"$;

$if(definitions)$
//определение
<- rrel_key_sc_element: ... (*
	<- definition;;
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

$if(relations)$
$relations:{rel| <= $rel.text$: $rel.conc$}; separator=";\n"$;
$endif$

<-sc_node_class;
<-class;
<-sc_node_not_relation;;