package com.jonavcar.tienda.entity;

import java.io.Serializable;
import java.math.BigDecimal;

public record Balance(
    BigDecimal amount,
    BigDecimal balance
) implements Serializable {}

