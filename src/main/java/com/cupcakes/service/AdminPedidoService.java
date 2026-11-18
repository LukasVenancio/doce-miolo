package com.cupcakes.service;

import java.util.List;

import com.cupcakes.dto.PedidoResponse;
import com.cupcakes.dto.PedidoResumoResponse;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.exception.TransicaoInvalidaException;
import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.HistoricoStatusPedido;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPagamento;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.repository.HistoricoStatusPedidoRepository;
import com.cupcakes.repository.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPedidoService {

    private final PedidoRepository pedidoRepository;
    private final HistoricoStatusPedidoRepository historicoRepository;
    private final PagamentoService pagamentoService;
    private final TransicaoStatusService transicaoStatusService;

    public AdminPedidoService(
        PedidoRepository pedidoRepository,
        HistoricoStatusPedidoRepository historicoRepository,
        PagamentoService pagamentoService,
        TransicaoStatusService transicaoStatusService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.historicoRepository = historicoRepository;
        this.pagamentoService = pagamentoService;
        this.transicaoStatusService = transicaoStatusService;
    }

    @Transactional(readOnly = true)
    public Page<PedidoResumoResponse> listar(StatusPedido status, Pageable pageable) {
        if (status == null) {
            return pedidoRepository.findAllByOrderByCriadoEmDesc(pageable).map(PedidoResumoResponse::de);
        }
        return pedidoRepository.findAllByStatusOrderByCriadoEmAsc(status, pageable)
                .map(PedidoResumoResponse::de);
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long id, StatusPedido status, String autor) {
        Pedido pedido = localizarPedido(id);
        validarPagamentoAntesDeMarcarPago(pedido, status);
        if (confirmarDinheiro(pedido, status)) {
            pagamentoService.confirmarComoAdministrador(pedido, autor);
            return salvarEResponder(pedido);
        }
        transicaoStatusService.transicionar(pedido, status, autor);
        return salvarEResponder(pedido);
    }

    private void validarPagamentoAntesDeMarcarPago(Pedido pedido, StatusPedido destino) {
        if (destino != StatusPedido.PAGO) {
            return;
        }
        validarStatusPagamento(pedido);
        validarFormaPagamento(pedido);
    }

    private void validarStatusPagamento(Pedido pedido) {
        if (pedido.getPagamento().getStatus() == StatusPagamento.PAGO) {
            return;
        }
        if (pedido.getPagamento().getStatus() != StatusPagamento.PENDENTE) {
            throw new TransicaoInvalidaException("O pagamento não está pendente.");
        }
    }

    private void validarFormaPagamento(Pedido pedido) {
        if (pedido.getPagamento().getStatus() == StatusPagamento.PAGO) {
            return;
        }
        if (pedido.getPagamento().getForma() == FormaPagamento.DINHEIRO) {
            return;
        }
        throw new TransicaoInvalidaException("Somente pagamentos em dinheiro podem ser confirmados pelo administrador.");
    }

    private boolean confirmarDinheiro(Pedido pedido, StatusPedido destino) {
        return destino == StatusPedido.PAGO
                && pedido.getPagamento().getForma() == FormaPagamento.DINHEIRO
                && pedido.getPagamento().getStatus() == StatusPagamento.PENDENTE;
    }

    private Pedido localizarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    private PedidoResponse salvarEResponder(Pedido pedido) {
        pedidoRepository.flush();
        List<HistoricoStatusPedido> historicos =
                historicoRepository.findAllByPedidoIdOrderByEmAsc(pedido.getId());
        return PedidoResponse.de(pedido, historicos);
    }
}