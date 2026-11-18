package com.cupcakes.repository;

import java.util.Optional;

import com.cupcakes.model.Pedido;
import com.cupcakes.model.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Override
    @EntityGraph(attributePaths = {"itens", "pagamento"})
    Optional<Pedido> findById(Long id);

    @EntityGraph(attributePaths = {"itens", "pagamento"})
    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);

    @EntityGraph(attributePaths = "pagamento")
    Page<Pedido> findAllByOrderByCriadoEmDesc(Pageable pageable);

    @EntityGraph(attributePaths = "pagamento")
    Page<Pedido> findAllByUsuarioIdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = "pagamento")
    Page<Pedido> findAllByStatusOrderByCriadoEmAsc(StatusPedido status, Pageable pageable);
}