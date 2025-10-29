package com.jonavcar.tienda.service;

import com.jonavcar.tienda.dao.VentaDao;
import com.jonavcar.tienda.dao.redis.OperationDaoRedis;
import com.jonavcar.tienda.model.Operation;
import com.jonavcar.tienda.model.Venta;
import com.jonavcar.tienda.service.kafka.VentaKafkaProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VentaService {

  private static final Logger LOG = Logger.getLogger(VentaService.class);

  @Inject
  VentaDao ventaDao;

  @Inject
  VentaKafkaProducer ventaKafkaProducer;

  @Inject
  OperationDaoRedis operationDaoRedis;

  public List<Venta> listar() {
    LOG.debug("Listing all ventas");
    return ventaDao.listar();
  }

  public Venta create(Venta venta) {
    LOG.infof("Creating new venta: clienteId=%s, total=%s", venta.getClienteId(), venta.getTotal());

    String sessionId = generateSessionId();
    String operationCode = generateOperationCode();

    try {
      // PASO 1: Guardar operación en Redis
      LOG.infof("Step 1: Saving operation to Redis - sessionId=%s, code=%s", sessionId, operationCode);
      Operation operation = createOperation(venta, sessionId, operationCode);
      boolean savedInRedis = operationDaoRedis.save(operation);

      if (!savedInRedis) {
        LOG.errorf("Failed to save operation in Redis for sessionId=%s", sessionId);
        throw new RuntimeException("Failed to save operation in Redis");
      }
      LOG.infof("Operation saved successfully in Redis: sessionId=%s", sessionId);

      // PASO 2: Enviar evento a Kafka de forma asíncrona
      LOG.infof("Step 2: Publishing event to Kafka for sessionId=%s", sessionId);
      publishEventAsync(venta, sessionId);

      // PASO 3: Persistir venta llamando al DAO (que internamente llama al proxy)
      LOG.infof("Step 3: Persisting venta via DAO for sessionId=%s", sessionId);
      Venta ventaCreada = ventaDao.create(venta);

      LOG.infof("Venta created successfully: id=%s, sessionId=%s", ventaCreada.getId(), sessionId);
      return ventaCreada;

    } catch (Exception e) {
      LOG.errorf(e, "Error creating venta for sessionId=%s", sessionId);
      // Intentar limpiar Redis en caso de error
      cleanupRedisOnError(sessionId, operationCode);
      throw new RuntimeException("Failed to create venta", e);
    }
  }

  /**
   * Publica el evento de venta creada de forma asíncrona
   * Los errores de Kafka no afectan la operación principal
   */
  private void publishEventAsync(Venta venta, String sessionId) {
    try {
      ventaKafkaProducer.publishVentaCreatedEvent(venta)
          .exceptionally(throwable -> {
            LOG.warnf(throwable, "Event publication failed for venta: clienteId=%s, sessionId=%s. Event will be lost.",
                venta.getClienteId(), sessionId);
            // TODO: Implementar patrón Outbox o Dead Letter Queue para eventos fallidos
            return null;
          });
    } catch (Exception e) {
      LOG.errorf(e, "Unexpected error publishing event for venta: clienteId=%s, sessionId=%s",
          venta.getClienteId(), sessionId);
      // No propagar la excepción para no afectar la operación principal
    }
  }

  /**
   * Crea la operación para guardar en Redis
   */
  private Operation createOperation(Venta venta, String sessionId, String operationCode) {
    Operation operation = new Operation();
    operation.setSessionId(sessionId);
    operation.setCodigo(operationCode);
    operation.setFecha(LocalDateTime.now());
    operation.setDescripcion(String.format("Venta - Cliente: %s, Total: %s",
        venta.getClienteId(), venta.getTotal()));
    return operation;
  }

  /**
   * Genera un ID único para la sesión
   */
  private String generateSessionId() {
    return "session-" + UUID.randomUUID();
  }

  /**
   * Genera un código único para la operación
   */
  private String generateOperationCode() {
    return "OP-" + System.currentTimeMillis();
  }

  /**
   * Limpia Redis en caso de error durante la creación de la venta
   */
  private void cleanupRedisOnError(String sessionId, String operationCode) {
    try {
      LOG.warnf("Cleaning up Redis for sessionId=%s, code=%s", sessionId, operationCode);
      operationDaoRedis.delete(sessionId, operationCode);
    } catch (Exception e) {
      LOG.errorf(e, "Failed to cleanup Redis for sessionId=%s", sessionId);
    }
  }
}
