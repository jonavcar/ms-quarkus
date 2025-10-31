package com.jonavcar.tienda.entity;

import java.io.Serializable;

public record Product(
    String id,
    String number,
    String status,
    Balance balance
) implements Serializable {}

