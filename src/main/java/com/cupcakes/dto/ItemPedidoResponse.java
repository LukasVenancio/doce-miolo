package com.cupcakes.dto;

import java.math.BigDecimal;

import com.cupcakes.model.ItemPedido;

public record ItemPedidoResponse(
        String nomeProduto,
        BigDecimal precoUnitario,
        int quantidade,
        String opcoesDescricao
) {

    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getNomeProduto(), item.getPrecoUnitario(), item.getQuantidade(), item.getOpcoesDescricao());
    }
}