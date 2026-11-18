package com.cupcakes.dto;

import java.math.BigDecimal;
import java.util.List;

public record ItemCarrinhoResponse(
        Long id,
        Long produtoId,
        String nomeProduto,
        int quantidade,
        List<OpcaoPersonalizacaoResponse> opcoes,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}