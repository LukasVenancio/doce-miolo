package com.cupcakes.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.cupcakes.dto.PedidoRequest;
import com.cupcakes.dto.PedidoResponse;
import com.cupcakes.dto.PedidoResumoResponse;
import com.cupcakes.exception.NegocioException;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.exception.ValidacaoNegocioException;
import com.cupcakes.model.Carrinho;
import com.cupcakes.model.ItemCarrinho;
import com.cupcakes.model.ItemPedido;
import com.cupcakes.model.OpcaoPersonalizacao;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.Produto;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.CarrinhoRepository;
import com.cupcakes.repository.HistoricoStatusPedidoRepository;
import com.cupcakes.repository.PedidoRepository;
import com.cupcakes.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private final CarrinhoRepository carrinhoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final HistoricoStatusPedidoRepository historicoRepository;
    private final FreteService freteService;
    private final PagamentoService pagamentoService;
    private final TransicaoStatusService transicaoStatusService;

    public PedidoService(
        CarrinhoRepository carrinhoRepository,
        UsuarioRepository usuarioRepository,
        PedidoRepository pedidoRepository,
        HistoricoStatusPedidoRepository historicoRepository,
        FreteService freteService,
        PagamentoService pagamentoService,
        TransicaoStatusService transicaoStatusService
    ) {
        this.carrinhoRepository = carrinhoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.historicoRepository = historicoRepository;
        this.freteService = freteService;
        this.pagamentoService = pagamentoService;
        this.transicaoStatusService = transicaoStatusService;
    }

    @Transactional
    public PedidoResponse criarPedido(String emailUsuario, PedidoRequest request) {
        Usuario usuario = localizarUsuario(emailUsuario);
        Carrinho carrinho = localizarCarrinho(usuario.getId());
        validarCarrinhoNaoVazio(carrinho);
        Pedido pedido = montarPedido(usuario, carrinho, request, emailUsuario);
        Pedido salvo = pedidoRepository.saveAndFlush(pedido);
        carrinho.getItens().clear();
        return PedidoResponse.de(salvo, salvo.getHistorico());
    }

    @Transactional(readOnly = true)
    public Page<PedidoResumoResponse> listarPedidos(String emailUsuario, Pageable pageable) {
        Usuario usuario = localizarUsuario(emailUsuario);
        return pedidoRepository.findAllByUsuarioIdOrderByCriadoEmDesc(usuario.getId(), pageable)
                .map(PedidoResumoResponse::de);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPedido(Long pedidoId, String emailUsuario) {
        Pedido pedido = localizarPedidoDoUsuario(pedidoId, emailUsuario);
        return respostaDetalhada(pedido);
    }

    @Transactional
    public PedidoResponse confirmarPagamento(Long pedidoId, String emailUsuario) {
        Pedido pedido = localizarPedidoDoUsuario(pedidoId, emailUsuario);
        pagamentoService.confirmarComoCliente(pedido, emailUsuario);
        pedidoRepository.flush();
        return respostaDetalhada(pedido);
    }

    @Transactional
    public PedidoResponse cancelarPedido(Long pedidoId, String emailUsuario) {
        Pedido pedido = localizarPedidoDoUsuario(pedidoId, emailUsuario);
        transicaoStatusService.cancelarComoCliente(pedido, emailUsuario);
        pagamentoService.cancelarPendente(pedido);
        pedidoRepository.flush();
        return respostaDetalhada(pedido);
    }

    private Pedido montarPedido(
        Usuario usuario,
        Carrinho carrinho,
        PedidoRequest request,
        String autor
    ) {
        Pedido pedido = novoPedido(usuario, request);
        adicionarItensSnapshot(pedido, carrinho);
        pedido.setTotal(calcularSubtotal(carrinho).add(pedido.getTaxaEntrega()));
        transicaoStatusService.registrarStatusInicial(pedido, autor);
        pagamentoService.iniciar(pedido, request.formaPagamento());
        return pedido;
    }

    private Pedido novoPedido(Usuario usuario, PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setTipoEntrega(request.tipoEntrega());
        pedido.setEnderecoSnapshot(enderecoSnapshot(request));
        pedido.setTaxaEntrega(freteService.calcular(request.tipoEntrega()));
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        return pedido;
    }

    private String enderecoSnapshot(PedidoRequest request) {
        if (request.tipoEntrega() == TipoEntrega.ENTREGA && request.endereco() == null) {
            throw new ValidacaoNegocioException("Endereço completo é obrigatório para entrega.");
        }
        if (request.tipoEntrega() == TipoEntrega.RETIRADA) {
            return null;
        }
        return request.endereco().snapshot();
    }

    private void adicionarItensSnapshot(Pedido pedido, Carrinho carrinho) {
        carrinho.getItens().stream()
                .map(item -> novoItemSnapshot(pedido, item))
                .forEach(pedido.getItens()::add);
    }

    private ItemPedido novoItemSnapshot(Pedido pedido, ItemCarrinho itemCarrinho) {
        ItemPedido itemPedido = new ItemPedido();
        itemPedido.setPedido(pedido);
        itemPedido.setNomeProduto(itemCarrinho.getProduto().getNome());
        itemPedido.setPrecoUnitario(calcularPrecoUnitario(itemCarrinho));
        itemPedido.setQuantidade(itemCarrinho.getQuantidade());
        itemPedido.setOpcoesDescricao(descricaoOpcoes(itemCarrinho));
        return itemPedido;
    }

    private BigDecimal calcularSubtotal(Carrinho carrinho) {
        return carrinho.getItens().stream()
                .map(this::calcularSubtotalItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularSubtotalItem(ItemCarrinho item) {
        return calcularPrecoUnitario(item).multiply(BigDecimal.valueOf(item.getQuantidade()));
    }

    private BigDecimal calcularPrecoUnitario(ItemCarrinho item) {
        return item.getOpcoes().stream()
                .map(OpcaoPersonalizacao::getPrecoAdicional)
                .reduce(item.getProduto().getPrecoBase(), BigDecimal::add);
    }

    private String descricaoOpcoes(ItemCarrinho item) {
        if (item.getOpcoes().isEmpty()) {
            return "Sem personalização";
        }
        return item.getOpcoes().stream()
                .sorted(Comparator.comparing(OpcaoPersonalizacao::getTipo)
                        .thenComparing(OpcaoPersonalizacao::getNome))
                .map(opcao -> opcao.getTipo().name() + ": " + opcao.getNome())
                .collect(Collectors.joining(", "));
    }

    private Usuario localizarUsuario(String emailUsuario) {
        return usuarioRepository.findByEmailIgnoreCase(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private Carrinho localizarCarrinho(Long usuarioId) {
        return carrinhoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NegocioException("O carrinho está vazio."));
    }

    private void validarCarrinhoNaoVazio(Carrinho carrinho) {
        if (carrinho.getItens().isEmpty()) {
            throw new NegocioException("O carrinho está vazio.");
        }
    }

    private Pedido localizarPedidoDoUsuario(Long pedidoId, String emailUsuario) {
        Long usuarioId = localizarUsuario(emailUsuario).getId();
        return pedidoRepository.findByIdAndUsuarioId(pedidoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    private PedidoResponse respostaDetalhada(Pedido pedido) {
        return PedidoResponse.de(
                pedido, historicoRepository.findAllByPedidoIdOrderByEmAsc(pedido.getId()));
    }
}