package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.List;

@ConfigMapping(prefix = "application.account-status-types-properties")
public interface AccountStatusTypesPropertiesConfig {

    @WithName("family-codes")
    FamilyCodes familyCodes();

    @WithName("join-account-type")
    JoinAccountType joinAccountType();

    @WithName("valid-states-accounts")
    List<ValidStatesAccount> validStatesAccounts();

    interface FamilyCodes {
        @WithName("invalid-code-origin")
        List<String> invalidCodeOrigin();

        @WithName("invalid-code-destination")
        List<String> invalidCodeDestination();
    }

    interface JoinAccountType {
        @WithName("invalid-code-origin")
        List<String> invalidCodeOrigin();

        @WithName("invalid-code-destination")
        List<String> invalidCodeDestination();
    }

    interface ValidStatesAccount {
        @WithName("family-codes")
        List<String> familyCodes();

        @WithName("valid-states")
        List<String> validStates();
    }
}

