package com.jonavcar.tienda.model;

import com.jonavcar.tienda.dto.SessionDto;
import java.util.Map;

public class SessionValidationResponse {

    private boolean valid;
    private SessionDto session;
    private Map<String, String> propagationHeaders;
    private String message;

    public SessionValidationResponse() {
    }

    public SessionValidationResponse(boolean valid, SessionDto session) {
        this.valid = valid;
        this.session = session;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public SessionDto getSession() {
        return session;
    }

    public void setSession(SessionDto session) {
        this.session = session;
    }

    public Map<String, String> getPropagationHeaders() {
        return propagationHeaders;
    }

    public void setPropagationHeaders(Map<String, String> propagationHeaders) {
        this.propagationHeaders = propagationHeaders;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

