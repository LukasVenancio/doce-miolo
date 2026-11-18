package com.cupcakes.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
        @NotNull @Positive Long categoriaId,
        @NotBlank @Size(max = 160) String nome,
        @NotBlank String descricao,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal precoBase,
        @Size(max = 1000) String imagemUrl,
        @NotNull Boolean personalizavel
) {
}