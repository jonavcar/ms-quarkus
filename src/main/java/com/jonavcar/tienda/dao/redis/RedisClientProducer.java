package com.jonavcar.tienda.dao.redis;

import com.jonavcar.tienda.config.RedisConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

@ApplicationScoped
public class RedisClientProducer {

  private static final Logger LOG = Logger.getLogger(RedisClientProducer.class);
  @Inject
  RedisConfig redisConfig;
  private RedissonClient redissonClient;

  void onStart(@Observes StartupEvent ev) {
    LOG.info("Initializing Redisson client on startup");
    this.redissonClient = createRedissonClient();
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (redissonClient != null && !redissonClient.isShutdown()) {
      LOG.info("Shutting down Redisson client");
      redissonClient.shutdown();
    }
  }

  @Produces
  @ApplicationScoped
  public RedissonClient produceRedissonClient() {
    if (redissonClient == null) {
      redissonClient = createRedissonClient();
    }
    return redissonClient;
  }

  private RedissonClient createRedissonClient() {
    LOG.info("Creating Redisson client with host: " + redisConfig.host() + " and port: " +
        redisConfig.port());

    Config config = new Config();
    String address = "redis://" + redisConfig.host() + ":" + redisConfig.port();

    config.useSingleServer()
        .setAddress(address)
        .setDatabase(redisConfig.database())
        .setTimeout(redisConfig.timeout())
        .setConnectionPoolSize(redisConfig.connectionPoolSize())
        .setConnectionMinimumIdleSize(redisConfig.connectionMinimumIdleSize())
        .setIdleConnectionTimeout(redisConfig.idleConnectionTimeout())
        .setConnectTimeout(redisConfig.connectTimeout())
        .setRetryAttempts(redisConfig.retryAttempts())
        .setRetryInterval(redisConfig.retryInterval());

    redisConfig.password()
        .filter(pwd -> !pwd.isEmpty())
        .ifPresent(pwd -> config.useSingleServer().setPassword(pwd));

    LOG.info("Redisson client created successfully");
    return Redisson.create(config);
  }
}
