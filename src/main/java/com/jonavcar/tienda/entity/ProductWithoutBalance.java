package com.jonavcar.tienda.entity;

import java.io.Serializable;

// DTO sin balance para consultas ligeras
public record ProductWithoutBalance(
    String id,
    String number,
    String status
) implements Serializable {}

