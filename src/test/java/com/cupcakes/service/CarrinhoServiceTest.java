package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.cupcakes.dto.CarrinhoResponse;
import com.cupcakes.dto.ItemCarrinhoRequest;
import com.cupcakes.dto.QuantidadeRequest;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.exception.ValidacaoNegocioException;
import com.cupcakes.model.Carrinho;
import com.cupcakes.model.ItemCarrinho;
import com.cupcakes.model.OpcaoPersonalizacao;
import com.cupcakes.model.Perfil;
import com.cupcakes.model.Produto;
import com.cupcakes.model.TipoOpcao;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.CarrinhoRepository;
import com.cupcakes.repository.OpcaoPersonalizacaoRepository;
import com.cupcakes.repository.ProdutoRepository;
import com.cupcakes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarrinhoServiceTest {

    @Mock
    private CarrinhoRepository carrinhoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private OpcaoPersonalizacaoRepository opcaoRepository;

    private CarrinhoService carrinhoService;
    private Usuario usuario;
    private Carrinho carrinho;
    private Produto produto;
    private List<OpcaoPersonalizacao> opcoes;

    @BeforeEach
    void preparar() {
        usuario = criarUsuario();
        carrinho = criarCarrinho();
        produto = criarProduto();
        opcoes = criarOpcoes();
        carrinhoService = new CarrinhoService(
                carrinhoRepository, usuarioRepository, produtoRepository, opcaoRepository);
        configurarRepositorios();
    }

    @Test
    void deve_calcular_preco_no_servidor_quando_adicionar_item() {
        configurarProduto();
        configurarCarrinhoExistente();
        when(opcaoRepository.findAllById(any())).thenReturn(opcoes);
        CarrinhoResponse response = carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 1, Set.of(1L, 2L, 3L)));

        assertEquals(new BigDecimal("15.00"), response.subtotal());
        assertEquals(new BigDecimal("15.00"), response.itens().getFirst().precoUnitario());
    }

    @Test
    void deve_mesclar_item_identico_quando_adicionar_opcoes_iguais() {
        configurarProduto();
        configurarCarrinhoExistente();
        when(opcaoRepository.findAllById(any())).thenReturn(opcoes);
        adicionarItemExistente();

        CarrinhoResponse response = carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 1, Set.of(1L, 2L, 3L)));

        assertEquals(1, response.itens().size());
        assertEquals(3, response.itens().getFirst().quantidade());
        assertEquals(new BigDecimal("45.00"), response.subtotal());
    }

    @Test
    void deve_rejeitar_opcao_obrigatoria_ausente_quando_personalizar() {
        configurarProduto();
        configurarCarrinhoExistente();
        when(opcaoRepository.findAllById(any())).thenReturn(opcoes.subList(0, 2));

        assertThrows(ValidacaoNegocioException.class, () -> carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 1, Set.of(1L, 2L))));
    }

    @Test
    void deve_rejeitar_opcoes_duplicadas_quando_tipo_nao_for_extra() {
        configurarProduto();
        configurarCarrinhoExistente();
        List<OpcaoPersonalizacao> opcoesDuplicadas = List.of(
                opcoes.get(0),
                criarOpcao(4L, TipoOpcao.MASSA, "Baunilha", "0.00"),
                opcoes.get(1),
                opcoes.get(2));
        when(opcaoRepository.findAllById(any())).thenReturn(opcoesDuplicadas);

        assertThrows(ValidacaoNegocioException.class, () -> carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 1, Set.of(1L, 2L, 3L, 4L))));
    }

    @Test
    void deve_ocultar_item_de_outro_carrinho_quando_atualizar_quantidade() {
        configurarCarrinhoExistente();
        assertThrows(RecursoNaoEncontradoException.class, () -> carrinhoService.atualizarQuantidade(
                usuario.getEmail(), 999L, new QuantidadeRequest(2)));
    }

    @Test
    void deve_retornar_carrinho_vazio_quando_usuario_nao_tiver_carrinho() {
        when(carrinhoRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.empty());

        CarrinhoResponse response = carrinhoService.buscar(usuario.getEmail());

        assertEquals(BigDecimal.ZERO, response.subtotal());
        assertEquals(0, response.itens().size());
    }

    @Test
    void deve_criar_carrinho_quando_usuario_adicionar_primeiro_item() {
        produto.setPersonalizavel(false);
        configurarProduto();
        when(carrinhoRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.empty());
        when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(opcaoRepository.findAllById(Set.of())).thenReturn(List.of());

        CarrinhoResponse response = carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 2, Set.of()));

        assertEquals(1, response.itens().size());
        assertEquals(new BigDecimal("24.00"), response.subtotal());
    }

    @Test
    void deve_atualizar_quantidade_quando_item_pertencer_ao_carrinho() {
        configurarCarrinhoExistente();
        adicionarItemExistente();

        CarrinhoResponse response = carrinhoService.atualizarQuantidade(
                usuario.getEmail(), 100L, new QuantidadeRequest(4));

        assertEquals(4, response.itens().getFirst().quantidade());
    }

    @Test
    void deve_remover_item_quando_item_pertencer_ao_carrinho() {
        configurarCarrinhoExistente();
        adicionarItemExistente();

        carrinhoService.removerItem(usuario.getEmail(), 100L);

        assertEquals(0, carrinho.getItens().size());
    }

    @Test
    void deve_rejeitar_ids_de_opcao_invalidos_quando_adicionar_item() {
        configurarProduto();
        configurarCarrinhoExistente();
        when(opcaoRepository.findAllById(Set.of(1L, 2L, 3L))).thenReturn(opcoes.subList(0, 2));

        assertThrows(ValidacaoNegocioException.class, () -> carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 1, Set.of(1L, 2L, 3L))));
    }

    @Test
    void deve_rejeitar_opcao_inativa_quando_adicionar_item() {
        configurarProduto();
        configurarCarrinhoExistente();
        OpcaoPersonalizacao opcaoInativa = criarOpcao(3L, TipoOpcao.COBERTURA, "Ganache", "1.50");
        opcaoInativa.setAtivo(false);
        when(opcaoRepository.findAllById(any())).thenReturn(List.of(opcoes.get(0), opcoes.get(1), opcaoInativa));

        assertThrows(ValidacaoNegocioException.class, () -> carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 1, Set.of(1L, 2L, 3L))));
    }

    @Test
    void deve_rejeitar_adicional_de_quantidade_acima_do_limite_quando_mesclar() {
        configurarProduto();
        configurarCarrinhoExistente();
        when(opcaoRepository.findAllById(any())).thenReturn(opcoes);
        adicionarItemExistente();
        carrinho.getItens().getFirst().setQuantidade(49);

        assertThrows(ValidacaoNegocioException.class, () -> carrinhoService.adicionarItem(
                usuario.getEmail(), new ItemCarrinhoRequest(20L, 2, Set.of(1L, 2L, 3L))));
    }

    private Usuario criarUsuario() {
        Usuario novoUsuario = new Usuario();
        novoUsuario.setId(10L);
        novoUsuario.setNome("Ana");
        novoUsuario.setEmail("ana@exemplo.com");
        novoUsuario.setSenhaHash("hash");
        novoUsuario.setPerfil(Perfil.CLIENTE);
        return novoUsuario;
    }

    private Carrinho criarCarrinho() {
        Carrinho novoCarrinho = new Carrinho();
        novoCarrinho.setId(11L);
        novoCarrinho.setUsuario(usuario);
        return novoCarrinho;
    }

    private Produto criarProduto() {
        Produto novoProduto = new Produto();
        novoProduto.setId(20L);
        novoProduto.setNome("Chocolate");
        novoProduto.setPrecoBase(new BigDecimal("12.00"));
        novoProduto.setPersonalizavel(true);
        novoProduto.setAtivo(true);
        return novoProduto;
    }

    private List<OpcaoPersonalizacao> criarOpcoes() {
        return List.of(
                criarOpcao(1L, TipoOpcao.MASSA, "Chocolate", "0.50"),
                criarOpcao(2L, TipoOpcao.RECHEIO, "Brigadeiro", "1.00"),
                criarOpcao(3L, TipoOpcao.COBERTURA, "Ganache", "1.50"));
    }

    private OpcaoPersonalizacao criarOpcao(Long id, TipoOpcao tipo, String nome, String preco) {
        OpcaoPersonalizacao opcao = new OpcaoPersonalizacao();
        opcao.setId(id);
        opcao.setTipo(tipo);
        opcao.setNome(nome);
        opcao.setPrecoAdicional(new BigDecimal(preco));
        opcao.setAtivo(true);
        return opcao;
    }

    private void configurarRepositorios() {
        when(usuarioRepository.findByEmailIgnoreCase(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    private void configurarCarrinhoExistente() {
        when(carrinhoRepository.findByUsuarioId(usuario.getId())).thenReturn(Optional.of(carrinho));
    }

    private void configurarProduto() {
        when(produtoRepository.findById(produto.getId())).thenReturn(Optional.of(produto));
    }

    private void adicionarItemExistente() {
        ItemCarrinho item = new ItemCarrinho();
        item.setId(100L);
        item.setCarrinho(carrinho);
        item.setProduto(produto);
        item.setQuantidade(2);
        item.getOpcoes().addAll(opcoes);
        carrinho.getItens().add(item);
    }
}