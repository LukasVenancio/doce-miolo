package com.cupcakes.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.cupcakes.dto.CarrinhoResponse;
import com.cupcakes.dto.ItemCarrinhoRequest;
import com.cupcakes.dto.ItemCarrinhoResponse;
import com.cupcakes.dto.OpcaoPersonalizacaoResponse;
import com.cupcakes.dto.QuantidadeRequest;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.exception.ValidacaoNegocioException;
import com.cupcakes.model.Carrinho;
import com.cupcakes.model.ItemCarrinho;
import com.cupcakes.model.OpcaoPersonalizacao;
import com.cupcakes.model.Produto;
import com.cupcakes.model.TipoOpcao;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.CarrinhoRepository;
import com.cupcakes.repository.OpcaoPersonalizacaoRepository;
import com.cupcakes.repository.ProdutoRepository;
import com.cupcakes.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final OpcaoPersonalizacaoRepository opcaoRepository;

    public CarrinhoService(
        CarrinhoRepository carrinhoRepository,
        UsuarioRepository usuarioRepository,
        ProdutoRepository produtoRepository,
        OpcaoPersonalizacaoRepository opcaoRepository
    ) {
        this.carrinhoRepository = carrinhoRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
        this.opcaoRepository = opcaoRepository;
    }

    @Transactional(readOnly = true)
    public CarrinhoResponse buscar(String emailUsuario) {
        Usuario usuario = localizarUsuario(emailUsuario);
        return carrinhoRepository.findByUsuarioId(usuario.getId())
                .map(this::resposta)
                .orElseGet(CarrinhoResponse::vazio);
    }

    @Transactional
    public CarrinhoResponse adicionarItem(String emailUsuario, ItemCarrinhoRequest request) {
        Carrinho carrinho = localizarOuCriarCarrinho(emailUsuario);
        Produto produto = localizarProdutoAtivo(request.produtoId());
        List<OpcaoPersonalizacao> opcoes = opcaoRepository.findAllById(request.opcoesIds());
        validarOpcoes(produto, opcoes, request.opcoesIds());
        Optional<ItemCarrinho> itemExistente = localizarItemIdentico(carrinho, produto, request.opcoesIds());
        if (itemExistente.isPresent()) {
            somarQuantidade(itemExistente.get(), request.quantidade());
            return resposta(carrinho);
        }
        carrinho.getItens().add(novoItem(carrinho, produto, request, opcoes));
        return resposta(carrinho);
    }

    @Transactional
    public CarrinhoResponse atualizarQuantidade(
        String emailUsuario,
        Long itemId,
        QuantidadeRequest request
    ) {
        Carrinho carrinho = localizarCarrinho(emailUsuario);
        localizarItem(carrinho, itemId).setQuantidade(request.quantidade());
        return resposta(carrinho);
    }

    @Transactional
    public void removerItem(String emailUsuario, Long itemId) {
        Carrinho carrinho = localizarCarrinho(emailUsuario);
        carrinho.getItens().remove(localizarItem(carrinho, itemId));
    }

    private Carrinho localizarOuCriarCarrinho(String emailUsuario) {
        Usuario usuario = localizarUsuario(emailUsuario);
        return carrinhoRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> criarCarrinho(usuario));
    }

    private Carrinho criarCarrinho(Usuario usuario) {
        Carrinho carrinho = new Carrinho();
        carrinho.setUsuario(usuario);
        return carrinhoRepository.save(carrinho);
    }

    private Carrinho localizarCarrinho(String emailUsuario) {
        Usuario usuario = localizarUsuario(emailUsuario);
        return carrinhoRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carrinho não encontrado."));
    }

    private Usuario localizarUsuario(String emailUsuario) {
        return usuarioRepository.findByEmailIgnoreCase(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private Produto localizarProdutoAtivo(Long produtoId) {
        return produtoRepository.findById(produtoId)
                .filter(Produto::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupcake não encontrado."));
    }

    private void validarOpcoes(
        Produto produto,
        List<OpcaoPersonalizacao> opcoes,
        Set<Long> opcoesIds
    ) {
        validarQuantidadeDeOpcoes(opcoes, opcoesIds);
        validarOpcoesAtivas(opcoes);
        validarPersonalizacaoDoProduto(produto, opcoes);
    }

    private void validarQuantidadeDeOpcoes(List<OpcaoPersonalizacao> opcoes, Set<Long> opcoesIds) {
        if (opcoes.size() != opcoesIds.size()) {
            throw new ValidacaoNegocioException("Uma ou mais opções de personalização são inválidas.");
        }
    }

    private void validarOpcoesAtivas(List<OpcaoPersonalizacao> opcoes) {
        if (opcoes.stream().anyMatch(opcao -> !opcao.isAtivo())) {
            throw new ValidacaoNegocioException("Opções inativas não podem ser selecionadas.");
        }
    }

    private void validarPersonalizacaoDoProduto(Produto produto, List<OpcaoPersonalizacao> opcoes) {
        if (!produto.isPersonalizavel() && !opcoes.isEmpty()) {
            throw new ValidacaoNegocioException("Este cupcake não permite personalização.");
        }
        if (!produto.isPersonalizavel()) {
            return;
        }
        validarUnicidadePorTipo(opcoes);
        validarTiposObrigatorios(opcoes);
    }

    private void validarUnicidadePorTipo(List<OpcaoPersonalizacao> opcoes) {
        boolean tiposDuplicados = opcoes.stream()
                .filter(opcao -> opcao.getTipo() != TipoOpcao.EXTRA)
                .collect(Collectors.groupingBy(OpcaoPersonalizacao::getTipo, Collectors.counting()))
                .values()
                .stream()
                .anyMatch(quantidade -> quantidade > 1);
        if (tiposDuplicados) {
            throw new ValidacaoNegocioException("Escolha no máximo uma opção de cada tipo, exceto extras.");
        }
    }

    private void validarTiposObrigatorios(List<OpcaoPersonalizacao> opcoes) {
        Set<TipoOpcao> tiposSelecionados = opcoes.stream()
                .map(OpcaoPersonalizacao::getTipo)
                .collect(Collectors.toSet());
        Set<TipoOpcao> tiposObrigatorios = EnumSet.of(
                TipoOpcao.MASSA, TipoOpcao.RECHEIO, TipoOpcao.COBERTURA);
        if (!tiposSelecionados.containsAll(tiposObrigatorios)) {
            throw new ValidacaoNegocioException("Escolha uma massa, um recheio e uma cobertura.");
        }
    }

    private Optional<ItemCarrinho> localizarItemIdentico(
        Carrinho carrinho,
        Produto produto,
        Set<Long> opcoesIds
    ) {
        return carrinho.getItens().stream()
                .filter(item -> mesmoItem(item, produto.getId(), opcoesIds))
                .findFirst();
    }

    private boolean mesmoItem(ItemCarrinho item, Long produtoId, Set<Long> opcoesIds) {
        Set<Long> opcoesExistentes = item.getOpcoes().stream()
                .map(OpcaoPersonalizacao::getId)
                .collect(Collectors.toSet());
        return item.getProduto().getId().equals(produtoId) && opcoesExistentes.equals(opcoesIds);
    }

    private void somarQuantidade(ItemCarrinho item, int quantidadeAdicionada) {
        int quantidadeTotal = item.getQuantidade() + quantidadeAdicionada;
        if (quantidadeTotal > 50) {
            throw new ValidacaoNegocioException("A quantidade máxima por item é 50.");
        }
        item.setQuantidade(quantidadeTotal);
    }

    private ItemCarrinho novoItem(
        Carrinho carrinho,
        Produto produto,
        ItemCarrinhoRequest request,
        List<OpcaoPersonalizacao> opcoes
    ) {
        ItemCarrinho item = new ItemCarrinho();
        item.setCarrinho(carrinho);
        item.setProduto(produto);
        item.setQuantidade(request.quantidade());
        item.getOpcoes().addAll(opcoes);
        return item;
    }

    private CarrinhoResponse resposta(Carrinho carrinho) {
        List<ItemCarrinhoResponse> itens = carrinho.getItens().stream()
                .map(this::respostaItem)
                .toList();
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinhoResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CarrinhoResponse(itens, subtotal);
    }

    private ItemCarrinhoResponse respostaItem(ItemCarrinho item) {
        BigDecimal precoUnitario = calcularPreco(item.getProduto(), item.getOpcoes());
        List<OpcaoPersonalizacaoResponse> opcoes = item.getOpcoes().stream()
                .sorted(java.util.Comparator.comparing(OpcaoPersonalizacao::getTipo)
                        .thenComparing(OpcaoPersonalizacao::getNome))
                .map(OpcaoPersonalizacaoResponse::de)
                .toList();
        BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(item.getQuantidade()));
        return new ItemCarrinhoResponse(
                item.getId(), item.getProduto().getId(), item.getProduto().getNome(),
                item.getQuantidade(), opcoes, precoUnitario, subtotal);
    }

    private BigDecimal calcularPreco(Produto produto, Set<OpcaoPersonalizacao> opcoes) {
        return opcoes.stream()
                .map(OpcaoPersonalizacao::getPrecoAdicional)
                .reduce(produto.getPrecoBase(), BigDecimal::add);
    }

    private ItemCarrinho localizarItem(Carrinho carrinho, Long itemId) {
        return carrinho.getItens().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no carrinho."));
    }
}