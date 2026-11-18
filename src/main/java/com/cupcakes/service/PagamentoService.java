package com.cupcakes.service;

import java.util.UUID;

import com.cupcakes.exception.TransicaoInvalidaException;
import com.cupcakes.exception.ValidacaoNegocioException;
import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.Pagamento;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPagamento;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {

    private final TransicaoStatusService transicaoStatusService;

    public PagamentoService(TransicaoStatusService transicaoStatusService) {
        this.transicaoStatusService = transicaoStatusService;
    }

    @Transactional
    public Pagamento iniciar(Pedido pedido, FormaPagamento forma) {
        validarFormaNaEntrega(pedido.getTipoEntrega(), forma);
        Pagamento pagamento = novoPagamento(pedido, forma);
        pedido.setPagamento(pagamento);
        return pagamento;
    }

    @Transactional
    public void confirmarComoCliente(Pedido pedido, String autor) {
        Pagamento pagamento = pedido.getPagamento();
        if (pagamento.getStatus() == StatusPagamento.PAGO) {
            return;
        }
        validarConfirmacaoPeloCliente(pagamento);
        pagamento.setStatus(StatusPagamento.PAGO);
        transicaoStatusService.transicionar(pedido, StatusPedido.PAGO, autor);
    }

    @Transactional
    public void confirmarComoAdministrador(Pedido pedido, String autor) {
        Pagamento pagamento = pedido.getPagamento();
        if (pagamento.getStatus() == StatusPagamento.PAGO) {
            return;
        }
        if (pagamento.getForma() != FormaPagamento.DINHEIRO) {
            throw new TransicaoInvalidaException("A confirmação administrativa é exclusiva para dinheiro.");
        }
        validarPagamentoPendente(pagamento);
        pagamento.setStatus(StatusPagamento.PAGO);
        transicaoStatusService.transicionar(pedido, StatusPedido.PAGO, autor);
    }

    @Transactional
    public void cancelarPendente(Pedido pedido) {
        Pagamento pagamento = pedido.getPagamento();
        if (pagamento.getStatus() == StatusPagamento.PENDENTE) {
            pagamento.setStatus(StatusPagamento.CANCELADO);
        }
    }

    private void validarFormaNaEntrega(TipoEntrega tipoEntrega, FormaPagamento forma) {
        if (forma == FormaPagamento.DINHEIRO && tipoEntrega != TipoEntrega.ENTREGA) {
            throw new ValidacaoNegocioException("Pagamento em dinheiro está disponível somente para entrega.");
        }
    }

    private Pagamento novoPagamento(Pedido pedido, FormaPagamento forma) {
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setForma(forma);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setValor(pedido.getTotal());
        pagamento.setCodigoPix(codigoPix(forma));
        return pagamento;
    }

    private String codigoPix(FormaPagamento forma) {
        if (forma != FormaPagamento.PIX) {
            return null;
        }
        return "PIX-SIMULADO-" + UUID.randomUUID();
    }

    private void validarConfirmacaoPeloCliente(Pagamento pagamento) {
        if (pagamento.getForma() == FormaPagamento.DINHEIRO) {
            throw new TransicaoInvalidaException("O pagamento em dinheiro deve ser confirmado pelo administrador.");
        }
        validarPagamentoPendente(pagamento);
    }

    private void validarPagamentoPendente(Pagamento pagamento) {
        if (pagamento.getStatus() != StatusPagamento.PENDENTE) {
            throw new TransicaoInvalidaException("O pagamento não está pendente.");
        }
    }
}