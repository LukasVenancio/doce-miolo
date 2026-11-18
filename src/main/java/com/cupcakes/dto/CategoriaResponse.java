package com.cupcakes.dto;

import com.cupcakes.model.Categoria;

public record CategoriaResponse(Long id, String nome) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome());
    }
}