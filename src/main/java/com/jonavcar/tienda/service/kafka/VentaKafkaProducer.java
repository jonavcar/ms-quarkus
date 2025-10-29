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

import java.util.concurrent.CompletionStage;

/**
 * Productor de eventos de Venta para Kafka
 * Maneja el envío de eventos del dominio Venta siguiendo patrones de Event-Driven Architecture
 */
@ApplicationScoped
public class VentaKafkaProducer {

    private static final Logger LOG = Logger.getLogger(VentaKafkaProducer.class);

    @Inject
    @Channel("ventas-out")
    Emitter<VentaEventDto> ventaEmitter;

    /**
     * Envía un evento de venta creada a Kafka de forma asíncrona
     *
     * @param venta La venta que se ha creado
     * @return CompletionStage que se completa cuando el mensaje se envía exitosamente
     * @throws IllegalArgumentException si venta es null o no tiene ID
     */
    public CompletionStage<Void> publishVentaCreatedEvent(Venta venta) {
        validateVenta(venta);

        VentaEventDto event = createEvent(venta);
        Message<VentaEventDto> message = buildKafkaMessage(event, venta.getId().toString());

        LOG.infof("Publishing venta.created event: eventId=%s, ventaId=%s, total=%s",
                event.getEventId(), venta.getId(), venta.getTotal());

        return ventaEmitter.send(message)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        LOG.errorf(throwable, "Failed to publish venta.created event: eventId=%s, ventaId=%s",
                                event.getEventId(), venta.getId());
                        throw new KafkaPublishException("Failed to publish event to Kafka", throwable);
                    }
                    LOG.infof("Successfully published venta.created event: eventId=%s, ventaId=%s",
                            event.getEventId(), venta.getId());
                    return null;
                });
    }

    /**
     * Envía un evento de venta de forma síncrona (espera confirmación)
     * No recomendado para operaciones críticas de rendimiento
     *
     * @param venta La venta que se ha creado
     * @throws KafkaPublishException si falla el envío
     */
    public void publishVentaCreatedEventSync(Venta venta) {
        try {
            publishVentaCreatedEvent(venta).toCompletableFuture().join();
        } catch (Exception e) {
            throw new KafkaPublishException("Failed to publish venta event synchronously", e);
        }
    }

    /**
     * Valida que la venta tenga los datos mínimos requeridos
     */
    private void validateVenta(Venta venta) {
        if (venta == null) {
            throw new IllegalArgumentException("Venta cannot be null");
        }
        if (venta.getId() == null) {
            throw new IllegalArgumentException("Venta ID cannot be null");
        }
    }

    /**
     * Crea el evento de dominio a partir de la venta
     */
    private VentaEventDto createEvent(Venta venta) {
        return new VentaEventDto(venta);
    }

    /**
     * Construye el mensaje de Kafka con metadata apropiada
     */
    private Message<VentaEventDto> buildKafkaMessage(VentaEventDto event, String key) {
        OutgoingKafkaRecordMetadata<String> metadata = OutgoingKafkaRecordMetadata.<String>builder()
                .withKey(key)
                .withHeader("event-type", event.getEventType().getBytes())
                .withHeader("event-version", event.getEventVersion().getBytes())
                .withHeader("event-id", event.getEventId().getBytes())
                .build();

        return Message.of(event).addMetadata(metadata);
    }

    /**
     * Excepción personalizada para errores de publicación en Kafka
     */
    public static class KafkaPublishException extends RuntimeException {
        public KafkaPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

