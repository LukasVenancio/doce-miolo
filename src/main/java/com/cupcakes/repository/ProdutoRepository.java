package com.cupcakes.repository;

import com.cupcakes.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @EntityGraph(attributePaths = "categoria")
    org.springframework.data.domain.Page<Produto> findAllByOrderByNomeAsc(Pageable pageable);

    @EntityGraph(attributePaths = "categoria")
    @Query("""
            SELECT produto
            FROM Produto produto
            WHERE produto.ativo = TRUE
              AND (:categoriaId IS NULL OR produto.categoria.id = :categoriaId)
              AND (:busca IS NULL OR LOWER(produto.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR LOWER(produto.descricao) LIKE LOWER(CONCAT('%', :busca, '%')))
            """)
    Page<Produto> buscarAtivos(
        @Param("busca") String busca,
        @Param("categoriaId") Long categoriaId,
        Pageable pageable
    );
}