package com.jonavcar.tienda.service.kafka;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

/**
 * Monitor de salud para los productores de Kafka
 * Verifica que los emitters estén listos durante el ciclo de vida de la aplicación
 */
@ApplicationScoped
public class KafkaHealthMonitor {

  private static final Logger LOG = Logger.getLogger(KafkaHealthMonitor.class);

  @Inject
  @Channel("ventas-out")
  Emitter<?> ventasEmitter;

  void onStart(@Observes StartupEvent event) {
    LOG.info("Kafka producers initialized");
    checkEmitterHealth("ventas-out", ventasEmitter);
  }

  void onStop(@Observes ShutdownEvent event) {
    LOG.info("Shutting down Kafka producers");
  }

  private void checkEmitterHealth(String channelName, Emitter<?> emitter) {
    if (emitter.isCancelled()) {
      LOG.warnf("Emitter for channel '%s' is cancelled", channelName);
    } else {
      LOG.infof("Emitter for channel '%s' is ready", channelName);
    }
  }
}

