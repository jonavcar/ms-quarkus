package com.jonavcar.tienda.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "application.transfer-limit-control")
public interface TransferLimitControlConfig {

  @WithName("capture-mode")
  CaptureMode captureMode();

  @WithName("flag-mode")
  FlagMode flagMode();

  @WithName("product-detail")
  ProductDetail productDetail();

  interface CaptureMode {
    @WithName("entry-mode")
    @WithDefault("01")
    String entryMode();
  }

  interface FlagMode {
    @WithName("lm10")
    @WithDefault("N")
    String lm10();

    @WithName("nslm")
    @WithDefault("N")
    String nslm();
  }

  interface ProductDetail {
    @WithName("family")
    Family family();

    @WithName("product")
    Product product();

    interface Family {
      @WithName("code")
      @WithDefault("015")
      String code();
    }

    interface Product {
      @WithName("code")
      @WithDefault("042")
      String code();
    }
  }
}

