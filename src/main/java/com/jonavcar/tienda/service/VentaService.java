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
    LOG.info("Creating new venta");

    // Crear la venta en la base de datos
    Venta ventaCreada = ventaDao.create(venta);

    // Enviar evento a Kafka de forma asíncrona
    ventaKafkaProducer.sendVentaCreatedEvent(ventaCreada)
        .thenAccept(v -> LOG.infof("Kafka event sent for venta: %s", ventaCreada.getId()))
        .exceptionally(throwable -> {
            LOG.errorf(throwable, "Failed to send Kafka event for venta: %s", ventaCreada.getId());
            // No falla la operación si Kafka falla, solo se loguea
            return null;
        });

    LOG.infof("Venta created successfully: %s", ventaCreada.getId());
    return ventaCreada;
  }
}
