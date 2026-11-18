package com.cupcakes.dto;

import com.cupcakes.model.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull StatusPedido status) {
}