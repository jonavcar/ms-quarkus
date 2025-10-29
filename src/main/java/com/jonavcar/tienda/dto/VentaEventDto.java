package com.jonavcar.tienda.dto;

import com.jonavcar.tienda.model.Venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VentaEventDto {

    private Long ventaId;
    private Long clienteId;
    private BigDecimal total;
    private String estado;
    private LocalDateTime fechaVenta;
    private String eventType;
    private LocalDateTime eventTimestamp;

    public VentaEventDto() {
        this.eventTimestamp = LocalDateTime.now();
    }

    public VentaEventDto(Venta venta, String eventType) {
        this.ventaId = venta.getId();
        this.clienteId = venta.getClienteId();
        this.total = venta.getTotal();
        this.estado = venta.getEstado();
        this.fechaVenta = venta.getFecha();
        this.eventType = eventType;
        this.eventTimestamp = LocalDateTime.now();
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @Override
    public String toString() {
        return "VentaEventDto{" +
                "ventaId=" + ventaId +
                ", clienteId=" + clienteId +
                ", total=" + total +
                ", estado='" + estado + '\'' +
                ", fechaVenta=" + fechaVenta +
                ", eventType='" + eventType + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}
