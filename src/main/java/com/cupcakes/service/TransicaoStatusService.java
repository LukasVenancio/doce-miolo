package com.cupcakes.service;

import java.util.Map;
import java.util.Set;

import com.cupcakes.exception.TransicaoInvalidaException;
import com.cupcakes.model.HistoricoStatusPedido;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransicaoStatusService {

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_VALIDAS = Map.of(
            StatusPedido.AGUARDANDO_PAGAMENTO, Set.of(StatusPedido.PAGO, StatusPedido.CANCELADO),
            StatusPedido.PAGO, Set.of(StatusPedido.EM_PREPARO, StatusPedido.CANCELADO),
            StatusPedido.EM_PREPARO, Set.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.ENTREGUE),
            StatusPedido.SAIU_PARA_ENTREGA, Set.of(StatusPedido.ENTREGUE));

    public void registrarStatusInicial(Pedido pedido, String autor) {
        registrarHistorico(pedido, pedido.getStatus(), autor);
    }

    @Transactional
    public void transicionar(Pedido pedido, StatusPedido destino, String autor) {
        validarTransicao(pedido, destino);
        validarTipoEntrega(pedido, destino);
        pedido.setStatus(destino);
        registrarHistorico(pedido, destino, autor);
    }

    @Transactional
    public void cancelarComoCliente(Pedido pedido, String autor) {
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new TransicaoInvalidaException("O cliente só pode cancelar antes da confirmação do pagamento.");
        }
        transicionar(pedido, StatusPedido.CANCELADO, autor);
    }

    private void validarTransicao(Pedido pedido, StatusPedido destino) {
        Set<StatusPedido> destinos = TRANSICOES_VALIDAS.getOrDefault(pedido.getStatus(), Set.of());
        if (!destinos.contains(destino)) {
            throw new TransicaoInvalidaException("Transição de status inválida.");
        }
    }

    private void validarTipoEntrega(Pedido pedido, StatusPedido destino) {
        if (destino == StatusPedido.SAIU_PARA_ENTREGA && pedido.getTipoEntrega() != TipoEntrega.ENTREGA) {
            throw new TransicaoInvalidaException("Retiradas não podem sair para entrega.");
        }
        if (destino == StatusPedido.ENTREGUE
                && pedido.getStatus() == StatusPedido.EM_PREPARO
                && pedido.getTipoEntrega() != TipoEntrega.RETIRADA) {
            throw new TransicaoInvalidaException("Pedidos de entrega devem sair para entrega antes.");
        }
    }

    private void registrarHistorico(Pedido pedido, StatusPedido status, String autor) {
        HistoricoStatusPedido historico = new HistoricoStatusPedido();
        historico.setPedido(pedido);
        historico.setStatus(status);
        historico.setAutor(autor);
        pedido.getHistorico().add(historico);
    }
}