package com.cupcakes.dto;

import java.math.BigDecimal;

import com.cupcakes.model.OpcaoPersonalizacao;
import com.cupcakes.model.TipoOpcao;

public record OpcaoPersonalizacaoResponse(Long id, TipoOpcao tipo, String nome, BigDecimal precoAdicional) {

    public static OpcaoPersonalizacaoResponse de(OpcaoPersonalizacao opcao) {
        return new OpcaoPersonalizacaoResponse(
                opcao.getId(), opcao.getTipo(), opcao.getNome(), opcao.getPrecoAdicional());
    }
}