package org.artrayme.translator.jafregle.translators;

import org.artrayme.translator.jafregle.http.HttpClient;
import org.artrayme.translator.jafregle.http.HttpMethod;
import org.artrayme.translator.jafregle.http.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GoogleApiTranslator implements Translator{

    final String GOOGLE_API_URL = "https://www.googleapis.com/language/translate/v2?";
    final String GOOGLE_API_PARAMS = "key=%s&q=%s&source=%s&target=%s";
    
    String googleKey;
    
    public GoogleApiTranslator(String googleKey)
    {
        this.googleKey = googleKey;
    }
    
    @Override
    public String requestTranslation(String textToTranslate, String from, String to) throws IllegalArgumentException, IOException {
        
        if(googleKey.isEmpty())
            throw new IllegalArgumentException("Google Key must to be informed.");
        
        String encodedText = java.net.URLEncoder.encode(textToTranslate, StandardCharsets.UTF_8);
        
        String params = String.format(GOOGLE_API_PARAMS, googleKey, encodedText, from, to);
        
        HttpResponse result = new HttpClient().request(HttpMethod.GET, GOOGLE_API_URL + params);
        return result.asString();
    }

}