package com.cupcakes.controller;

import java.util.List;

import com.cupcakes.dto.CategoriaResponse;
import com.cupcakes.dto.OpcaoPersonalizacaoResponse;
import com.cupcakes.service.CatalogoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/api/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return catalogoService.listarCategorias();
    }

    @GetMapping("/api/opcoes-personalizacao")
    public List<OpcaoPersonalizacaoResponse> listarOpcoes() {
        return catalogoService.listarOpcoesAtivas();
    }
}