package com.cupcakes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.HistoricoStatusPedido;
import com.cupcakes.model.Pagamento;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPagamento;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;

public record PedidoResponse(
        Long id,
        TipoEntrega tipoEntrega,
        String enderecoSnapshot,
        BigDecimal taxaEntrega,
        BigDecimal total,
        StatusPedido status,
        LocalDateTime criadoEm,
        List<ItemPedidoResponse> itens,
        FormaPagamento formaPagamento,
        StatusPagamento statusPagamento,
        String codigoPix,
        List<HistoricoStatusResponse> historico
) {

    public static PedidoResponse de(Pedido pedido, List<HistoricoStatusPedido> historicos) {
        Pagamento pagamento = pedido.getPagamento();
        return new PedidoResponse(
                pedido.getId(), pedido.getTipoEntrega(), pedido.getEnderecoSnapshot(),
                pedido.getTaxaEntrega(), pedido.getTotal(), pedido.getStatus(), pedido.getCriadoEm(),
                itens(pedido), pagamento.getForma(), pagamento.getStatus(), pagamento.getCodigoPix(),
                historicos.stream().map(HistoricoStatusResponse::de).toList());
    }

    private static List<ItemPedidoResponse> itens(Pedido pedido) {
        return pedido.getItens().stream().map(ItemPedidoResponse::de).toList();
    }
}