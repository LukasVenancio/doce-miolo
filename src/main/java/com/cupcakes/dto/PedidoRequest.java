package com.cupcakes.dto;

import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.TipoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PedidoRequest(
        @NotNull TipoEntrega tipoEntrega,
        @Valid EnderecoPedidoRequest endereco,
        @NotNull FormaPagamento formaPagamento
) {
}