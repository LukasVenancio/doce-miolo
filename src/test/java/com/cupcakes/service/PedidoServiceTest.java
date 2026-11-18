package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.cupcakes.config.FreteProperties;
import com.cupcakes.dto.EnderecoPedidoRequest;
import com.cupcakes.dto.PedidoRequest;
import com.cupcakes.exception.NegocioException;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.exception.ValidacaoNegocioException;
import com.cupcakes.model.Carrinho;
import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.ItemCarrinho;
import com.cupcakes.model.OpcaoPersonalizacao;
import com.cupcakes.model.Pagamento;
import com.cupcakes.model.Pedido;
import com.cupcakes.model.Produto;
import com.cupcakes.model.StatusPagamento;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import com.cupcakes.model.TipoOpcao;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.CarrinhoRepository;
import com.cupcakes.repository.HistoricoStatusPedidoRepository;
import com.cupcakes.repository.PedidoRepository;
import com.cupcakes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private CarrinhoRepository carrinhoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private HistoricoStatusPedidoRepository historicoRepository;

    private PedidoService pedidoService;
    private Usuario usuario;
    private Carrinho carrinho;

    @BeforeEach
    void preparar() {
        usuario = criarUsuario();
        carrinho = criarCarrinho();
        FreteProperties properties = new FreteProperties();
        properties.setTaxa(new BigDecimal("8.00"));
        TransicaoStatusService transicaoStatusService = new TransicaoStatusService();
        pedidoService = new PedidoService(
                carrinhoRepository,
                usuarioRepository,
                pedidoRepository,
                historicoRepository,
                new FreteService(properties),
                new PagamentoService(transicaoStatusService),
                transicaoStatusService);
    }

    @Test
    void deve_gravar_snapshot_e_total_quando_criar_pedido() {
        configurarUsuarioECarrinho();
        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var pedido = pedidoService.criarPedido(usuario.getEmail(), pedidoEntrega());

        assertEquals(new BigDecimal("28.00"), pedido.total());
        assertEquals("Rua Central, 10, Ap 2, Centro, São Paulo, SP, 01000-000", pedido.enderecoSnapshot());
        assertEquals("Sem personalização", pedido.itens().getFirst().opcoesDescricao());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.status());
        assertEquals(StatusPagamento.PENDENTE, pedido.statusPagamento());
        assertEquals(0, carrinho.getItens().size());
    }

    @Test
    void deve_rejeitar_checkout_quando_carrinho_vazio() {
        configurarUsuarioECarrinho();
        carrinho.getItens().clear();

        assertThrows(NegocioException.class,
                () -> pedidoService.criarPedido(usuario.getEmail(), pedidoEntrega()));
    }

    @Test
    void deve_retornar_nao_encontrado_quando_pedido_for_de_outro_usuario() {
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(pedidoRepository.findByIdAndUsuarioId(99L, usuario.getId())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> pedidoService.buscarPedido(99L, usuario.getEmail()));
    }

    @Test
    void deve_listar_pedidos_paginados_quando_cliente_consultar_historico() {
        Pedido pedido = criarPedidoPersistido();
        var pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(pedidoRepository.findAllByUsuarioIdOrderByCriadoEmDesc(usuario.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        var pagina = pedidoService.listarPedidos(usuario.getEmail(), pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(40L, pagina.getContent().getFirst().id());
    }

    @Test
    void deve_confirmar_pagamento_quando_cliente_confirmar_pix() {
        Pedido pedido = criarPedidoPersistido();
        pedido.getPagamento().setForma(FormaPagamento.PIX);
        configurarPedidoDoUsuario(pedido);

        var response = pedidoService.confirmarPagamento(pedido.getId(), usuario.getEmail());

        assertEquals(StatusPedido.PAGO, response.status());
        assertEquals(StatusPagamento.PAGO, response.statusPagamento());
    }

    @Test
    void deve_cancelar_pedido_e_pagamento_quando_cliente_cancelar_pedido_pendente() {
        Pedido pedido = criarPedidoPersistido();
        configurarPedidoDoUsuario(pedido);

        var response = pedidoService.cancelarPedido(pedido.getId(), usuario.getEmail());

        assertEquals(StatusPedido.CANCELADO, response.status());
        assertEquals(StatusPagamento.CANCELADO, response.statusPagamento());
    }

    @Test
    void deve_criar_retirada_sem_endereco_e_sem_frete() {
        configurarUsuarioECarrinho();
        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PedidoRequest request = new PedidoRequest(TipoEntrega.RETIRADA, null, FormaPagamento.CARTAO);

        var response = pedidoService.criarPedido(usuario.getEmail(), request);

        assertNull(response.enderecoSnapshot());
        assertEquals(BigDecimal.ZERO, response.taxaEntrega());
    }

    @Test
    void deve_rejeitar_entrega_sem_endereco_quando_criar_pedido() {
        configurarUsuarioECarrinho();
        PedidoRequest request = new PedidoRequest(TipoEntrega.ENTREGA, null, FormaPagamento.PIX);

        assertThrows(ValidacaoNegocioException.class,
                () -> pedidoService.criarPedido(usuario.getEmail(), request));
    }

    @Test
    void deve_gravar_snapshot_de_opcoes_quando_criar_pedido_personalizado() {
        configurarUsuarioECarrinho();
        ItemCarrinho item = carrinho.getItens().getFirst();
        item.getProduto().setPersonalizavel(true);
        item.getOpcoes().add(criarOpcao());
        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = pedidoService.criarPedido(usuario.getEmail(), pedidoEntrega());

        assertEquals("MASSA: Baunilha", response.itens().getFirst().opcoesDescricao());
        assertEquals(new BigDecimal("28.50"), response.total());
    }

    private void configurarUsuarioECarrinho() {
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(carrinhoRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(carrinho));
    }

    private Usuario criarUsuario() {
        Usuario novoUsuario = new Usuario();
        novoUsuario.setId(1L);
        novoUsuario.setEmail("ana@exemplo.com");
        return novoUsuario;
    }

    private Carrinho criarCarrinho() {
        Carrinho novoCarrinho = new Carrinho();
        novoCarrinho.setId(2L);
        novoCarrinho.setUsuario(usuario);
        novoCarrinho.getItens().add(criarItem());
        return novoCarrinho;
    }

    private ItemCarrinho criarItem() {
        Produto produto = new Produto();
        produto.setId(3L);
        produto.setNome("Cupcake simples");
        produto.setPrecoBase(new BigDecimal("10.00"));
        produto.setPersonalizavel(false);
        ItemCarrinho item = new ItemCarrinho();
        item.setProduto(produto);
        item.setQuantidade(2);
        return item;
    }

    private PedidoRequest pedidoEntrega() {
        EnderecoPedidoRequest endereco = new EnderecoPedidoRequest(
                "Rua Central", "10", "Ap 2", "Centro", "São Paulo", "SP", "01000-000");
        return new PedidoRequest(TipoEntrega.ENTREGA, endereco, FormaPagamento.PIX);
    }

    private Pedido criarPedidoPersistido() {
        Pedido pedido = new Pedido();
        pedido.setId(40L);
        pedido.setUsuario(usuario);
        pedido.setTipoEntrega(TipoEntrega.ENTREGA);
        pedido.setTotal(new BigDecimal("20.00"));
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setForma(FormaPagamento.PIX);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setValor(pedido.getTotal());
        pedido.setPagamento(pagamento);
        return pedido;
    }

    private void configurarPedidoDoUsuario(Pedido pedido) {
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(pedidoRepository.findByIdAndUsuarioId(pedido.getId(), usuario.getId())).thenReturn(Optional.of(pedido));
        when(historicoRepository.findAllByPedidoIdOrderByEmAsc(pedido.getId()))
                .thenAnswer(invocation -> pedido.getHistorico());
    }

    private OpcaoPersonalizacao criarOpcao() {
        OpcaoPersonalizacao opcao = new OpcaoPersonalizacao();
        opcao.setId(5L);
        opcao.setTipo(TipoOpcao.MASSA);
        opcao.setNome("Baunilha");
        opcao.setPrecoAdicional(new BigDecimal("0.25"));
        opcao.setAtivo(true);
        return opcao;
    }
}