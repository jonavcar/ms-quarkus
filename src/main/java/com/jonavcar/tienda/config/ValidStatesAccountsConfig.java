package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.List;

@ConfigMapping(prefix = "application.account-status-types-properties.valid-states-accounts")
public interface ValidStatesAccountsConfig {

  List<ValidStateAccount> validStates();

  interface ValidStateAccount {
    @WithName("family-codes")
    List<String> familyCodes();

    @WithName("valid-states")
    List<String> validStates();
  }
}

