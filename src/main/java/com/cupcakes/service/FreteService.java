package com.cupcakes.service;

import java.math.BigDecimal;

import com.cupcakes.config.FreteProperties;
import com.cupcakes.model.TipoEntrega;
import org.springframework.stereotype.Service;

@Service
public class FreteService {

    private final FreteProperties freteProperties;

    public FreteService(FreteProperties freteProperties) {
        this.freteProperties = freteProperties;
    }

    public BigDecimal calcular(TipoEntrega tipoEntrega) {
        if (tipoEntrega == TipoEntrega.RETIRADA) {
            return BigDecimal.ZERO;
        }
        return freteProperties.getTaxa();
    }
}