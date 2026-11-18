package com.cupcakes.repository;

import java.util.List;

import com.cupcakes.model.OpcaoPersonalizacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpcaoPersonalizacaoRepository extends JpaRepository<OpcaoPersonalizacao, Long> {

    List<OpcaoPersonalizacao> findAllByAtivoTrueOrderByTipoAscNomeAsc();
}