package com.cupcakes.controller;

import com.cupcakes.dto.ProdutoRequest;
import com.cupcakes.dto.ProdutoResponse;
import com.cupcakes.service.AdminProdutoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/produtos")
public class AdminProdutoController {

    private final AdminProdutoService adminProdutoService;

    public AdminProdutoController(AdminProdutoService adminProdutoService) {
        this.adminProdutoService = adminProdutoService;
    }

    @GetMapping
    public Page<ProdutoResponse> listar(
        @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return adminProdutoService.listar(pageable);
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable @Positive Long id) {
        return adminProdutoService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criar(@Valid @RequestBody ProdutoRequest request) {
        return adminProdutoService.criar(request);
    }

    @PutMapping("/{id}")
    public ProdutoResponse atualizar(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ProdutoRequest request
    ) {
        return adminProdutoService.atualizar(id, request);
    }

    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable @Positive Long id) {
        adminProdutoService.inativar(id);
    }
}