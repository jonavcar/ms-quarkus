package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;
import java.util.Map;

@ConfigMapping(prefix = "application.adapter.signModeToDescription")
public interface SignModeToDescriptionConfig {

  @WithParentName
  Map<Integer, String> modes();
}

