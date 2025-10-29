package com.jonavcar.tienda.dao.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonavcar.tienda.model.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jboss.logging.Logger;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

@ApplicationScoped
public class OperationDaoRedis {

  private static final Logger LOG = Logger.getLogger(OperationDaoRedis.class);
  private static final String REDIS_KEY_PREFIX = "session:";

  @Inject
  RedissonClient redissonClient;

  @Inject
  ObjectMapper objectMapper;

  /**
   * Guarda una operación en Redis usando el sessionId como clave en un mapa Hash
   *
   * @param operation La operación a guardar
   * @return true si se guardó correctamente, false en caso contrario
   * @throws IllegalArgumentException si operation es null o sus campos requeridos están vacíos
   */
  public boolean save(Operation operation) {
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }
    if (operation.getSessionId() == null || operation.getSessionId().isEmpty()) {
      throw new IllegalArgumentException("SessionId cannot be null or empty");
    }
    if (operation.getCodigo() == null || operation.getCodigo().isEmpty()) {
      throw new IllegalArgumentException("Codigo cannot be null or empty");
    }

    try {
      String key = buildKey(operation.getSessionId());
      String operationJson = objectMapper.writeValueAsString(operation);

      LOG.infof("Saving operation with codigo: %s for session: %s", operation.getCodigo(),
          operation.getSessionId());

      RMap<String, String> map = redissonClient.getMap(key);
      map.put(operation.getCodigo(), operationJson);

      LOG.infof("Operation saved successfully with codigo: %s", operation.getCodigo());
      return true;
    } catch (JsonProcessingException e) {
      LOG.errorf(e, "Error serializing operation to JSON: %s", operation.getCodigo());
      return false;
    } catch (Exception e) {
      LOG.errorf(e, "Error saving operation to Redis: %s", operation.getCodigo());
      return false;
    }
  }

  /**
   * Recupera una operación específica por sessionId y código
   *
   * @param sessionId El ID de sesión
   * @param codigo    El código de la operación
   * @return Optional con la operación si existe, Optional.empty() si no existe
   * @throws IllegalArgumentException si sessionId o codigo son null o vacíos
   */
  public Optional<Operation> recovery(String sessionId, String codigo) {
    validateSessionId(sessionId);
    validateCodigo(codigo);

    try {
      String key = buildKey(sessionId);

      LOG.debugf("Recovering operation with codigo: %s for session: %s", codigo, sessionId);

      RMap<String, String> map = redissonClient.getMap(key);
      String operationJson = map.get(codigo);

      if (operationJson == null || operationJson.isEmpty()) {
        LOG.debugf("Operation not found with codigo: %s for session: %s", codigo, sessionId);
        return Optional.empty();
      }

      Operation operation = objectMapper.readValue(operationJson, Operation.class);

      LOG.infof("Operation recovered successfully with codigo: %s", codigo);
      return Optional.of(operation);
    } catch (Exception e) {
      LOG.errorf(e, "Error recovering operation from Redis with codigo: %s", codigo);
      return Optional.empty();
    }
  }

  /**
   * Recupera todas las operaciones de una sesión
   *
   * @param sessionId El ID de sesión
   * @return Lista de operaciones de la sesión (nunca null, puede estar vacía)
   * @throws IllegalArgumentException si sessionId es null o vacío
   */
  public List<Operation> recoveryAllBySession(String sessionId) {
    validateSessionId(sessionId);

    try {
      String key = buildKey(sessionId);

      LOG.debugf("Recovering all operations for session: %s", sessionId);

      RMap<String, String> map = redissonClient.getMap(key);

      if (!map.isExists()) {
        LOG.debugf("No operations found for session: %s", sessionId);
        return Collections.emptyList();
      }

      Map<String, String> allOperations = map.readAllMap();

      if (allOperations.isEmpty()) {
        LOG.debugf("No operations found for session: %s", sessionId);
        return Collections.emptyList();
      }

      List<Operation> operations = new ArrayList<>(allOperations.size());

      for (Map.Entry<String, String> entry : allOperations.entrySet()) {
        try {
          Operation operation = objectMapper.readValue(entry.getValue(), Operation.class);
          operations.add(operation);
        } catch (Exception e) {
          LOG.warnf(e, "Error deserializing operation with codigo: %s, skipping", entry.getKey());
        }
      }

      LOG.infof("Recovered %d operations for session: %s", operations.size(), sessionId);
      return operations;
    } catch (Exception e) {
      LOG.errorf(e, "Error recovering operations from Redis for session: %s", sessionId);
      return Collections.emptyList();
    }
  }

  /**
   * Elimina una operación específica por sessionId y código
   *
   * @param sessionId El ID de sesión
   * @param codigo    El código de la operación
   * @return true si se eliminó correctamente, false si no existe o hubo error
   * @throws IllegalArgumentException si sessionId o codigo son null o vacíos
   */
  public boolean delete(String sessionId, String codigo) {
    validateSessionId(sessionId);
    validateCodigo(codigo);

    try {
      String key = buildKey(sessionId);

      LOG.infof("Deleting operation with codigo: %s for session: %s", codigo, sessionId);

      RMap<String, String> map = redissonClient.getMap(key);
      String removed = map.remove(codigo);

      if (removed != null) {
        LOG.infof("Operation deleted successfully with codigo: %s", codigo);
        return true;
      } else {
        LOG.debugf("Operation not found or already deleted with codigo: %s", codigo);
        return false;
      }
    } catch (Exception e) {
      LOG.errorf(e, "Error deleting operation from Redis with codigo: %s", codigo);
      return false;
    }
  }

  /**
   * Elimina todas las operaciones de una sesión
   *
   * @param sessionId El ID de sesión
   * @return true si se eliminó correctamente, false si no existe o hubo error
   * @throws IllegalArgumentException si sessionId es null o vacío
   */
  public boolean deleteAllBySession(String sessionId) {
    validateSessionId(sessionId);

    try {
      String key = buildKey(sessionId);

      LOG.infof("Deleting all operations for session: %s", sessionId);

      RMap<String, String> map = redissonClient.getMap(key);

      if (!map.isExists()) {
        LOG.debugf("Session not found or already deleted: %s", sessionId);
        return false;
      }

      boolean deleted = map.delete();

      if (deleted) {
        LOG.infof("All operations deleted successfully for session: %s", sessionId);
      } else {
        LOG.warnf("Failed to delete session: %s", sessionId);
      }

      return deleted;
    } catch (Exception e) {
      LOG.errorf(e, "Error deleting session from Redis: %s", sessionId);
      return false;
    }
  }

  /**
   * Verifica si existe una sesión con operaciones
   *
   * @param sessionId El ID de sesión
   * @return true si existe, false en caso contrario
   */
  public boolean existsSession(String sessionId) {
    validateSessionId(sessionId);

    try {
      String key = buildKey(sessionId);
      RMap<String, String> map = redissonClient.getMap(key);
      return map.isExists();
    } catch (Exception e) {
      LOG.errorf(e, "Error checking session existence: %s", sessionId);
      return false;
    }
  }

  /**
   * Cuenta el número de operaciones en una sesión
   *
   * @param sessionId El ID de sesión
   * @return número de operaciones, 0 si no existe la sesión
   */
  public int countOperations(String sessionId) {
    validateSessionId(sessionId);

    try {
      String key = buildKey(sessionId);
      RMap<String, String> map = redissonClient.getMap(key);
      return map.size();
    } catch (Exception e) {
      LOG.errorf(e, "Error counting operations for session: %s", sessionId);
      return 0;
    }
  }

  // Métodos de utilidad privados

  private String buildKey(String sessionId) {
    return REDIS_KEY_PREFIX + sessionId;
  }

  private void validateSessionId(String sessionId) {
    if (sessionId == null || sessionId.trim().isEmpty()) {
      throw new IllegalArgumentException("SessionId cannot be null or empty");
    }
  }

  private void validateCodigo(String codigo) {
    if (codigo == null || codigo.trim().isEmpty()) {
      throw new IllegalArgumentException("Codigo cannot be null or empty");
    }
  }
}

