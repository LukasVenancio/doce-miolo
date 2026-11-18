package com.cupcakes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPagamento;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;

public record PedidoResumoResponse(
        Long id,
        TipoEntrega tipoEntrega,
        BigDecimal total,
        StatusPedido status,
        LocalDateTime criadoEm,
        FormaPagamento formaPagamento,
        StatusPagamento statusPagamento
) {

    public static PedidoResumoResponse de(Pedido pedido) {
        return new PedidoResumoResponse(
            pedido.getId(),
            pedido.getTipoEntrega(),
            pedido.getTotal(),
            pedido.getStatus(),
            pedido.getCriadoEm(),
            pedido.getPagamento().getForma(),
            pedido.getPagamento().getStatus());
    }
}