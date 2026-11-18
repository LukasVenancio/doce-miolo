package com.cupcakes.service;

import java.util.List;

import com.cupcakes.dto.CategoriaResponse;
import com.cupcakes.dto.OpcaoPersonalizacaoResponse;
import com.cupcakes.repository.CategoriaRepository;
import com.cupcakes.repository.OpcaoPersonalizacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final OpcaoPersonalizacaoRepository opcaoRepository;

    public CatalogoService(CategoriaRepository categoriaRepository, OpcaoPersonalizacaoRepository opcaoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.opcaoRepository = opcaoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaRepository.findAllByOrderByNomeAsc().stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OpcaoPersonalizacaoResponse> listarOpcoesAtivas() {
        return opcaoRepository.findAllByAtivoTrueOrderByTipoAscNomeAsc().stream()
                .map(OpcaoPersonalizacaoResponse::de)
                .toList();
    }
}