package com.jonavcar.tienda.dto;

import java.io.Serializable;
import java.util.Map;

public class SessionDto implements Serializable {

    private String sessionId;
    private String userId;
    private String tenantId;
    private String username;
    private Map<String, String> metadata;
    private Long expiresAt;

    public SessionDto() {
    }

    public SessionDto(String sessionId, String userId, String tenantId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.tenantId = tenantId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
}

