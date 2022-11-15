package org.artrayme.translator.jafregle.translators;

import java.io.IOException;

public interface Translator {
    
    String requestTranslation(String textToTranslate, String from, String to) throws IOException;
    
}
