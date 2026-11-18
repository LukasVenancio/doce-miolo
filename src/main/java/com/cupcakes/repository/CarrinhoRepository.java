package com.cupcakes.repository;

import java.util.Optional;

import com.cupcakes.model.Carrinho;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    Optional<Carrinho> findByUsuarioId(Long usuarioId);
}