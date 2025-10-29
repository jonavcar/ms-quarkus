package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import java.util.List;
import java.util.Map;

@ConfigMapping(prefix = "application.authorization.validations")
public interface ValidationsConfig {

  @WithParentName
  Map<String, ChannelValidations> channels();

  interface ChannelValidations {
    @WithParentName
    Map<String, List<String>> operations();
  }
}

