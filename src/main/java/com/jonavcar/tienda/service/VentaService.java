package com.jonavcar.tienda.service;

import com.jonavcar.tienda.dao.VentaDao;
import com.jonavcar.tienda.model.Venta;
import com.jonavcar.tienda.service.kafka.VentaKafkaProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VentaService {

  private static final Logger LOG = Logger.getLogger(VentaService.class);

  @Inject
  VentaDao ventaDao;

  @Inject
  VentaKafkaProducer ventaKafkaProducer;

  public List<Venta> listar() {
    LOG.debug("Listing all ventas");
    return ventaDao.listar();
  }

  public Venta create(Venta venta) {
    LOG.infof("Creating new venta: clienteId=%s, total=%s", venta.getClienteId(), venta.getTotal());

    // Persistir la venta primero
    Venta ventaCreada = ventaDao.create(venta);

    // Publicar evento de forma asíncrona sin bloquear la respuesta
    publishEventAsync(ventaCreada);

    LOG.infof("Venta created successfully: id=%s", ventaCreada.getId());
    return ventaCreada;
  }

  /**
   * Publica el evento de venta creada de forma asíncrona
   * Los errores de Kafka no afectan la operación principal
   */
  private void publishEventAsync(Venta venta) {
    try {
      ventaKafkaProducer.publishVentaCreatedEvent(venta)
          .exceptionally(throwable -> {
            LOG.warnf(throwable, "Event publication failed for venta: id=%s. Event will be lost.",
                venta.getId());
            // TODO: Implementar patrón Outbox o Dead Letter Queue para eventos fallidos
            return null;
          });
    } catch (Exception e) {
      LOG.errorf(e, "Unexpected error publishing event for venta: id=%s", venta.getId());
      // No propagar la excepción para no afectar la operación principal
    }
  }
}
