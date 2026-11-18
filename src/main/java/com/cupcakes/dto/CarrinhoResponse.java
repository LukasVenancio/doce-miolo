package com.cupcakes.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarrinhoResponse(List<ItemCarrinhoResponse> itens, BigDecimal subtotal) {

    public static CarrinhoResponse vazio() {
        return new CarrinhoResponse(List.of(), BigDecimal.ZERO);
    }
}