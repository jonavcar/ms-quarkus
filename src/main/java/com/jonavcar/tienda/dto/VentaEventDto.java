package com.jonavcar.tienda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jonavcar.tienda.event.DomainEvent;
import com.jonavcar.tienda.model.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VentaEventDto extends DomainEvent {

    private static final String EVENT_TYPE = "venta.created";
    private static final String EVENT_VERSION = "1.0";

    @JsonProperty("venta_id")
    private Long ventaId;

    @JsonProperty("cliente_id")
    private Long clienteId;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("estado")
    private String estado;

    @JsonProperty("fecha_venta")
    private LocalDateTime fechaVenta;

    public VentaEventDto() {
        super(EVENT_TYPE, EVENT_VERSION);
    }

    public VentaEventDto(Venta venta) {
        super(EVENT_TYPE, EVENT_VERSION);
        this.ventaId = venta.getId();
        this.clienteId = venta.getClienteId();
        this.total = venta.getTotal();
        this.estado = venta.getEstado();
        this.fechaVenta = venta.getFecha();
    }

    public Long getVentaId() {
        return ventaId;
    }

    public void setVentaId(Long ventaId) {
        this.ventaId = ventaId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDateTime fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    @Override
    public String toString() {
        return "VentaEventDto{" +
                "eventId='" + getEventId() + '\'' +
                ", eventType='" + getEventType() + '\'' +
                ", ventaId=" + ventaId +
                ", clienteId=" + clienteId +
                ", total=" + total +
                ", estado='" + estado + '\'' +
                ", fechaVenta=" + fechaVenta +
                ", eventTimestamp=" + getEventTimestamp() +
                '}';
    }
}

