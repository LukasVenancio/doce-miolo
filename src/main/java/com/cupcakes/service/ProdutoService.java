package com.cupcakes.service;

import com.cupcakes.dto.ProdutoResponse;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.model.Produto;
import com.cupcakes.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listarAtivos(String busca, Long categoriaId, Pageable pageable) {
        return produtoRepository.buscarAtivos(normalizarBusca(busca), categoriaId, pageable)
                .map(ProdutoResponse::de);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarAtivo(Long id) {
        return produtoRepository.findById(id)
                .filter(Produto::isAtivo)
                .map(ProdutoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupcake não encontrado."));
    }

    private String normalizarBusca(String busca) {
        if (busca == null) {
            return null;
        }
        return busca.strip();
    }
}