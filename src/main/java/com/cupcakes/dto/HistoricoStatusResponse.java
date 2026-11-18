package com.cupcakes.dto;

import java.time.LocalDateTime;

import com.cupcakes.model.HistoricoStatusPedido;
import com.cupcakes.model.StatusPedido;

public record HistoricoStatusResponse(StatusPedido status, LocalDateTime em, String autor) {

    public static HistoricoStatusResponse de(HistoricoStatusPedido historico) {
        return new HistoricoStatusResponse(historico.getStatus(), historico.getEm(), historico.getAutor());
    }
}