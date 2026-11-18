package com.cupcakes.repository;

import java.util.List;

import com.cupcakes.model.HistoricoStatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusPedidoRepository extends JpaRepository<HistoricoStatusPedido, Long> {

    List<HistoricoStatusPedido> findAllByPedidoIdOrderByEmAsc(Long pedidoId);
}