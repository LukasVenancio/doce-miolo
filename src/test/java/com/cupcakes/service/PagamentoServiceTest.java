package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.cupcakes.exception.TransicaoInvalidaException;
import com.cupcakes.exception.ValidacaoNegocioException;
import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.Pagamento;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPagamento;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import org.junit.jupiter.api.Test;

class PagamentoServiceTest {

    private final TransicaoStatusService transicaoStatusService = new TransicaoStatusService();
    private final PagamentoService pagamentoService = new PagamentoService(transicaoStatusService);

    @Test
    void deve_confirmar_pix_quando_cliente_confirmar_pagamento() {
        Pedido pedido = criarPedido(TipoEntrega.ENTREGA, FormaPagamento.PIX);

        pagamentoService.confirmarComoCliente(pedido, "ana@exemplo.com");

        assertEquals(StatusPagamento.PAGO, pedido.getPagamento().getStatus());
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        assertEquals(StatusPedido.PAGO, pedido.getHistorico().getFirst().getStatus());
    }

    @Test
    void deve_rejeitar_dinheiro_na_retirada_quando_iniciar_pagamento() {
        Pedido pedido = criarPedido(TipoEntrega.RETIRADA, FormaPagamento.PIX);

        assertThrows(ValidacaoNegocioException.class,
                () -> pagamentoService.iniciar(pedido, FormaPagamento.DINHEIRO));
    }

    @Test
    void deve_rejeitar_confirmacao_de_dinheiro_pelo_cliente() {
        Pedido pedido = criarPedido(TipoEntrega.ENTREGA, FormaPagamento.DINHEIRO);

        assertThrows(TransicaoInvalidaException.class,
                () -> pagamentoService.confirmarComoCliente(pedido, "ana@exemplo.com"));
    }

    @Test
    void deve_confirmar_dinheiro_quando_administrador_marcar_pago() {
        Pedido pedido = criarPedido(TipoEntrega.ENTREGA, FormaPagamento.DINHEIRO);

        pagamentoService.confirmarComoAdministrador(pedido, "admin@exemplo.com");

        assertEquals(StatusPagamento.PAGO, pedido.getPagamento().getStatus());
        assertEquals(StatusPedido.PAGO, pedido.getStatus());
    }

    @Test
    void deve_manter_pago_quando_cliente_repetir_confirmacao() {
        Pedido pedido = criarPedido(TipoEntrega.ENTREGA, FormaPagamento.PIX);
        pedido.getPagamento().setStatus(StatusPagamento.PAGO);
        pedido.setStatus(StatusPedido.PAGO);

        pagamentoService.confirmarComoCliente(pedido, "ana@exemplo.com");

        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        assertEquals(0, pedido.getHistorico().size());
    }

    @Test
    void deve_rejeitar_confirmacao_admin_para_pix() {
        Pedido pedido = criarPedido(TipoEntrega.ENTREGA, FormaPagamento.PIX);

        assertThrows(TransicaoInvalidaException.class,
                () -> pagamentoService.confirmarComoAdministrador(pedido, "admin@exemplo.com"));
    }

    @Test
    void deve_cancelar_pagamento_pendente_quando_pedido_for_cancelado() {
        Pedido pedido = criarPedido(TipoEntrega.ENTREGA, FormaPagamento.PIX);

        pagamentoService.cancelarPendente(pedido);

        assertEquals(StatusPagamento.CANCELADO, pedido.getPagamento().getStatus());
    }

    @Test
    void deve_preservar_pagamento_pago_quando_cancelar_pendente() {
        Pedido pedido = criarPedido(TipoEntrega.ENTREGA, FormaPagamento.PIX);
        pedido.getPagamento().setStatus(StatusPagamento.PAGO);

        pagamentoService.cancelarPendente(pedido);

        assertEquals(StatusPagamento.PAGO, pedido.getPagamento().getStatus());
    }

    private Pedido criarPedido(TipoEntrega tipoEntrega, FormaPagamento formaPagamento) {
        Pedido pedido = new Pedido();
        pedido.setTipoEntrega(tipoEntrega);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setTotal(new BigDecimal("20.00"));
        pagamentoService.iniciar(pedido, formaPagamento);
        return pedido;
    }
}