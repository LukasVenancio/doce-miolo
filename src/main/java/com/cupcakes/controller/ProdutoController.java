package com.cupcakes.controller;

import com.cupcakes.dto.ProdutoResponse;
import com.cupcakes.service.ProdutoService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public Page<ProdutoResponse> listar(
        @RequestParam(required = false) @Size(max = 100) String busca,
        @RequestParam(required = false) @Positive Long categoriaId,
        @PageableDefault(size = 12, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return produtoService.listarAtivos(busca, categoriaId, pageable);
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable @Positive Long id) {
        return produtoService.buscarAtivo(id);
    }
}