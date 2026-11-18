package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cupcakes.exception.TransicaoInvalidaException;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import org.junit.jupiter.api.Test;

class TransicaoStatusServiceTest {

    private final TransicaoStatusService service = new TransicaoStatusService();

    @Test
    void deve_gravar_historico_quando_mudar_status() {
        Pedido pedido = criarPedido(StatusPedido.AGUARDANDO_PAGAMENTO, TipoEntrega.ENTREGA);

        service.transicionar(pedido, StatusPedido.PAGO, "ana@exemplo.com");

        assertEquals(StatusPedido.PAGO, pedido.getStatus());
        assertEquals(1, pedido.getHistorico().size());
        assertEquals("ana@exemplo.com", pedido.getHistorico().getFirst().getAutor());
    }

    @Test
    void deve_rejeitar_transicao_quando_destino_nao_for_permitido() {
        Pedido pedido = criarPedido(StatusPedido.ENTREGUE, TipoEntrega.RETIRADA);

        assertThrows(TransicaoInvalidaException.class,
                () -> service.transicionar(pedido, StatusPedido.EM_PREPARO, "admin@exemplo.com"));
    }

    @Test
    void deve_rejeitar_entrega_direta_quando_pedido_for_de_entrega() {
        Pedido pedido = criarPedido(StatusPedido.EM_PREPARO, TipoEntrega.ENTREGA);

        assertThrows(TransicaoInvalidaException.class,
                () -> service.transicionar(pedido, StatusPedido.ENTREGUE, "admin@exemplo.com"));
    }

    @Test
    void deve_registrar_status_inicial_quando_pedido_for_criado() {
        Pedido pedido = criarPedido(StatusPedido.AGUARDANDO_PAGAMENTO, TipoEntrega.ENTREGA);

        service.registrarStatusInicial(pedido, "ana@exemplo.com");

        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getHistorico().getFirst().getStatus());
        assertEquals("ana@exemplo.com", pedido.getHistorico().getFirst().getAutor());
    }

    @Test
    void deve_permitir_saida_para_entrega_quando_tipo_for_entrega() {
        Pedido pedido = criarPedido(StatusPedido.EM_PREPARO, TipoEntrega.ENTREGA);

        service.transicionar(pedido, StatusPedido.SAIU_PARA_ENTREGA, "admin@exemplo.com");

        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, pedido.getStatus());
    }

    @Test
    void deve_permitir_entrega_imediata_quando_tipo_for_retirada() {
        Pedido pedido = criarPedido(StatusPedido.EM_PREPARO, TipoEntrega.RETIRADA);

        service.transicionar(pedido, StatusPedido.ENTREGUE, "admin@exemplo.com");

        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
    }

    @Test
    void deve_rejeitar_saida_para_entrega_quando_tipo_for_retirada() {
        Pedido pedido = criarPedido(StatusPedido.EM_PREPARO, TipoEntrega.RETIRADA);

        assertThrows(TransicaoInvalidaException.class,
                () -> service.transicionar(pedido, StatusPedido.SAIU_PARA_ENTREGA, "admin@exemplo.com"));
    }

    @Test
    void deve_rejeitar_cancelamento_do_cliente_quando_pedido_ja_pago() {
        Pedido pedido = criarPedido(StatusPedido.PAGO, TipoEntrega.ENTREGA);

        assertThrows(TransicaoInvalidaException.class,
                () -> service.cancelarComoCliente(pedido, "ana@exemplo.com"));
    }

    private Pedido criarPedido(StatusPedido status, TipoEntrega tipoEntrega) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setTipoEntrega(tipoEntrega);
        return pedido;
    }
}