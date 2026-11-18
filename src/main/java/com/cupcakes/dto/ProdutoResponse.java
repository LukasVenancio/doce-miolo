package com.cupcakes.dto;

import java.math.BigDecimal;

import com.cupcakes.model.Produto;

public record ProdutoResponse(
        Long id,
        Long categoriaId,
        String categoriaNome,
        String nome,
        String descricao,
        BigDecimal precoBase,
        String imagemUrl,
        boolean personalizavel,
        boolean ativo
) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getCategoria().getId(),
                produto.getCategoria().getNome(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPrecoBase(),
                produto.getImagemUrl(),
                produto.isPersonalizavel(),
                produto.isAtivo());
    }
}