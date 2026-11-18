package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import com.cupcakes.config.FreteProperties;
import com.cupcakes.model.TipoEntrega;
import org.junit.jupiter.api.Test;

class FreteServiceTest {

    @Test
    void deve_retornar_taxa_configurada_quando_for_entrega() {
        FreteProperties properties = new FreteProperties();
        properties.setTaxa(new BigDecimal("9.50"));

        assertEquals(new BigDecimal("9.50"), new FreteService(properties).calcular(TipoEntrega.ENTREGA));
    }

    @Test
    void deve_retornar_zero_quando_for_retirada() {
        FreteProperties properties = new FreteProperties();
        properties.setTaxa(new BigDecimal("9.50"));

        assertEquals(BigDecimal.ZERO, new FreteService(properties).calcular(TipoEntrega.RETIRADA));
    }
}