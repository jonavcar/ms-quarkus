package com.jonavcar.tienda.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Clase base para todos los eventos del dominio
 */
public abstract class DomainEvent {

  @JsonProperty("event_id")
  private final String eventId;

  @JsonProperty("event_type")
  private final String eventType;

  @JsonProperty("event_timestamp")
  private final Instant eventTimestamp;

  @JsonProperty("event_version")
  private final String eventVersion;

  protected DomainEvent(String eventType, String eventVersion) {
    this.eventId = UUID.randomUUID().toString();
    this.eventType = eventType;
    this.eventTimestamp = Instant.now();
    this.eventVersion = eventVersion;
  }

  public String getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public Instant getEventTimestamp() {
    return eventTimestamp;
  }

  public String getEventVersion() {
    return eventVersion;
  }

  @Override
  public String toString() {
    return "DomainEvent{" +
        "eventId='" + eventId + '\'' +
        ", eventType='" + eventType + '\'' +
        ", eventTimestamp=" + eventTimestamp +
        ", eventVersion='" + eventVersion + '\'' +
        '}';
  }
}

