package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.cupcakes.exception.TransicaoInvalidaException;
import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.Pagamento;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPagamento;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import com.cupcakes.repository.HistoricoStatusPedidoRepository;
import com.cupcakes.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminPedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private HistoricoStatusPedidoRepository historicoRepository;

    private AdminPedidoService service;
    private Pedido pedido;

    @BeforeEach
    void preparar() {
        PagamentoService pagamentoService = new PagamentoService(new TransicaoStatusService());
        service = new AdminPedidoService(
                pedidoRepository,
                historicoRepository,
                pagamentoService,
                new TransicaoStatusService());
        pedido = criarPedido();
    }

    @Test
    void deve_confirmar_dinheiro_quando_admin_atualizar_para_pago() {
        when(pedidoRepository.findById(40L)).thenReturn(Optional.of(pedido));
        when(historicoRepository.findAllByPedidoIdOrderByEmAsc(40L))
                .thenAnswer(invocation -> pedido.getHistorico());

        var response = service.atualizarStatus(40L, StatusPedido.PAGO, "admin@exemplo.com");

        assertEquals(StatusPedido.PAGO, response.status());
        assertEquals(StatusPagamento.PAGO, response.statusPagamento());
    }

    @Test
    void deve_rejeitar_pagamento_cancelado_quando_admin_marcar_pago() {
        pedido.getPagamento().setStatus(StatusPagamento.CANCELADO);
        when(pedidoRepository.findById(40L)).thenReturn(Optional.of(pedido));

        assertThrows(TransicaoInvalidaException.class,
                () -> service.atualizarStatus(40L, StatusPedido.PAGO, "admin@exemplo.com"));
    }

    @Test
    void deve_listar_todos_pedidos_quando_status_nao_for_informado() {
        var pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findAllByOrderByCriadoEmDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        var pagina = service.listar(null, pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(FormaPagamento.DINHEIRO, pagina.getContent().getFirst().formaPagamento());
    }

    @Test
    void deve_filtrar_pedidos_por_status_quando_status_for_informado() {
        var pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findAllByStatusOrderByCriadoEmAsc(StatusPedido.AGUARDANDO_PAGAMENTO, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        var pagina = service.listar(StatusPedido.AGUARDANDO_PAGAMENTO, pageable);

        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pagina.getContent().getFirst().status());
    }

    @Test
    void deve_mudar_para_em_preparo_quando_admin_confirmar_status_pago() {
        pedido.setStatus(StatusPedido.PAGO);
        pedido.getPagamento().setForma(FormaPagamento.PIX);
        pedido.getPagamento().setStatus(StatusPagamento.PAGO);
        configurarPedidoEHistorico();

        var response = service.atualizarStatus(40L, StatusPedido.EM_PREPARO, "admin@exemplo.com");

        assertEquals(StatusPedido.EM_PREPARO, response.status());
    }

    @Test
    void deve_rejeitar_pix_pendente_quando_admin_tentar_marcar_pago() {
        pedido.getPagamento().setForma(FormaPagamento.PIX);
        when(pedidoRepository.findById(40L)).thenReturn(Optional.of(pedido));

        assertThrows(TransicaoInvalidaException.class,
                () -> service.atualizarStatus(40L, StatusPedido.PAGO, "admin@exemplo.com"));
    }

    @Test
    void deve_lancar_nao_encontrado_quando_admin_consultar_pedido_ausente() {
        when(pedidoRepository.findById(40L)).thenReturn(Optional.empty());

        assertThrows(com.cupcakes.exception.RecursoNaoEncontradoException.class,
                () -> service.atualizarStatus(40L, StatusPedido.PAGO, "admin@exemplo.com"));
    }

    private void configurarPedidoEHistorico() {
        when(pedidoRepository.findById(40L)).thenReturn(Optional.of(pedido));
        when(historicoRepository.findAllByPedidoIdOrderByEmAsc(40L))
                .thenAnswer(invocation -> pedido.getHistorico());
    }

    private Pedido criarPedido() {
        Pedido novoPedido = new Pedido();
        novoPedido.setId(40L);
        novoPedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        novoPedido.setTipoEntrega(TipoEntrega.ENTREGA);
        novoPedido.setTotal(new BigDecimal("20.00"));
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(novoPedido);
        pagamento.setForma(FormaPagamento.DINHEIRO);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setValor(new BigDecimal("20.00"));
        novoPedido.setPagamento(pagamento);
        return novoPedido;
    }
}