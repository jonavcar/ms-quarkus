package com.jonavcar.tienda.context;

import jakarta.enterprise.context.RequestScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequestScoped
public class EndpointHeaderContext {

    private final ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();

    public void addHeader(String key, String value) {
        if (key != null && value != null) {
            headers.put(key, value);
        }
    }

    public void addHeaders(Map<String, String> headersToAdd) {
        if (headersToAdd != null) {
            headers.putAll(headersToAdd);
        }
    }

    public Map<String, String> extractAndClear() {
        Map<String, String> copy = Map.copyOf(headers);
        headers.clear();
        return copy;
    }

    public boolean hasHeaders() {
        return !headers.isEmpty();
    }

    public int size() {
        return headers.size();
    }
}

