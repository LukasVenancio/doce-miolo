package com.cupcakes.dto;

import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemCarrinhoRequest(
        @NotNull @Positive Long produtoId,
        @NotNull @Min(1) @Max(50) Integer quantidade,
        @NotNull Set<@NotNull @Positive Long> opcoesIds
) {
}