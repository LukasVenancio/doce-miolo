package com.cupcakes.service;

import com.cupcakes.dto.ProdutoRequest;
import com.cupcakes.dto.ProdutoResponse;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.model.Categoria;
import com.cupcakes.model.Produto;
import com.cupcakes.repository.CategoriaRepository;
import com.cupcakes.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public AdminProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(Pageable pageable) {
        return produtoRepository.findAllByOrderByNomeAsc(pageable).map(ProdutoResponse::de);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return ProdutoResponse.de(localizarProduto(id));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto();
        atualizarCampos(produto, request);
        return ProdutoResponse.de(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = localizarProduto(id);
        atualizarCampos(produto, request);
        return ProdutoResponse.de(produtoRepository.save(produto));
    }

    @Transactional
    public void inativar(Long id) {
        Produto produto = localizarProduto(id);
        produto.setAtivo(false);
    }

    private void atualizarCampos(Produto produto, ProdutoRequest request) {
        produto.setCategoria(localizarCategoria(request.categoriaId()));
        produto.setNome(request.nome().strip());
        produto.setDescricao(request.descricao().strip());
        produto.setPrecoBase(request.precoBase());
        produto.setImagemUrl(request.imagemUrl());
        produto.setPersonalizavel(request.personalizavel());
    }

    private Produto localizarProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupcake não encontrado."));
    }

    private Categoria localizarCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }
}