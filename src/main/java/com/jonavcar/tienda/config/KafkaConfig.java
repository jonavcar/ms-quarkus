package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "kafka")
public interface KafkaConfig {

  @WithName("bootstrap.servers")
  @WithDefault("localhost:9092")
  String bootstrapServers();

  Topics topics();

  @ConfigMapping(prefix = "topics")
  interface Topics {

    @WithName("ventas")
    @WithDefault("ventas-events")
    String ventas();
  }
}
