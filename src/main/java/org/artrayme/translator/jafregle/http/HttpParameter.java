package org.artrayme.translator.jafregle.http;

public class HttpParameter {

    private final String name;
    private final String value;

    public HttpParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

}
