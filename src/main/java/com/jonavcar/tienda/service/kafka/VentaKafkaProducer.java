package com.jonavcar.tienda.service.kafka;

import com.jonavcar.tienda.dto.VentaEventDto;
import com.jonavcar.tienda.model.Venta;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class VentaKafkaProducer {

    private static final Logger LOG = Logger.getLogger(VentaKafkaProducer.class);
    private static final String EVENT_TYPE_CREATED = "VENTA_CREATED";

    @Inject
    @Channel("ventas")
    Emitter<VentaEventDto> ventaEmitter;

    /**
     * Envía un evento de venta creada a Kafka
     * @param venta La venta que se ha creado
     * @return CompletableFuture<Void> que se completa cuando el mensaje se envía exitosamente
     */
    public CompletableFuture<Void> sendVentaCreatedEvent(Venta venta) {
        if (venta == null) {
            LOG.error("Cannot send event for null venta");
            return CompletableFuture.failedFuture(new IllegalArgumentException("Venta cannot be null"));
        }

        try {
            VentaEventDto eventDto = new VentaEventDto(venta, EVENT_TYPE_CREATED);

            LOG.infof("Sending venta created event to Kafka: ventaId=%s, total=%s",
                    venta.getId(), venta.getTotal());

            // Crear metadata para el mensaje de Kafka
            String key = venta.getId() != null ? venta.getId().toString() : "unknown";
            OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(key)
                    .build();

            // Crear el mensaje con metadata
            Message<VentaEventDto> message = Message.of(eventDto)
                    .addMetadata(metadata);

            // Enviar el mensaje de forma asíncrona
            return ventaEmitter.send(message)
                    .thenRun(() -> LOG.infof("Venta event sent successfully: ventaId=%s", venta.getId()))
                    .exceptionally(throwable -> {
                        LOG.errorf(throwable, "Error sending venta event to Kafka: ventaId=%s", venta.getId());
                        return null;
                    });

        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error creating venta event: ventaId=%s", venta.getId());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Envía un evento de venta de forma síncrona (espera confirmación)
     * @param venta La venta que se ha creado
     */
    public void sendVentaCreatedEventSync(Venta venta) {
        try {
            sendVentaCreatedEvent(venta).join();
        } catch (Exception e) {
            LOG.errorf(e, "Error sending venta event synchronously: ventaId=%s", venta.getId());
            throw new RuntimeException("Failed to send venta event to Kafka", e);
        }
    }

    /**
     * Verifica si el emitter está listo para enviar mensajes
     * @return true si está listo, false en caso contrario
     */
    public boolean isReady() {
        return !ventaEmitter.isCancelled() && !ventaEmitter.hasRequests();
    }
}
