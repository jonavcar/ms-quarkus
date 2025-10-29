package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.Optional;

@ConfigMapping(prefix = "redis")
public interface RedisConfig {

  @WithName("host")
  @WithDefault("localhost")
  String host();

  @WithName("port")
  @WithDefault("6379")
  Integer port();

  @WithName("password")
  Optional<String> password();

  @WithName("timeout")
  @WithDefault("10000")
  Integer timeout();

  @WithName("database")
  @WithDefault("0")
  Integer database();

  @WithName("connection-pool-size")
  @WithDefault("64")
  Integer connectionPoolSize();

  @WithName("connection-minimum-idle-size")
  @WithDefault("10")
  Integer connectionMinimumIdleSize();

  @WithName("idle-connection-timeout")
  @WithDefault("10000")
  Integer idleConnectionTimeout();

  @WithName("connect-timeout")
  @WithDefault("10000")
  Integer connectTimeout();

  @WithName("retry-attempts")
  @WithDefault("3")
  Integer retryAttempts();

  @WithName("retry-interval")
  @WithDefault("1500")
  Integer retryInterval();
}
