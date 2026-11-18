package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.model.Categoria;
import com.cupcakes.model.Produto;
import com.cupcakes.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    private ProdutoService produtoService;

    @BeforeEach
    void preparar() {
        produtoService = new ProdutoService(produtoRepository);
    }

    @Test
    void deve_remover_espacos_da_busca_quando_listar_produtos() {
        Produto produto = produtoAtivo();
        var pageable = PageRequest.of(0, 10);
        when(produtoRepository.buscarAtivos("chocolate", 4L, pageable))
                .thenReturn(new PageImpl<>(List.of(produto), pageable, 1));

        var pagina = produtoService.listarAtivos(" chocolate ", 4L, pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Chocolate", pagina.getContent().getFirst().nome());
        verify(produtoRepository).buscarAtivos("chocolate", 4L, pageable);
    }

    @Test
    void deve_retornar_produto_ativo_quando_buscar_por_id() {
        when(produtoRepository.findById(12L)).thenReturn(Optional.of(produtoAtivo()));

        assertEquals("Chocolate", produtoService.buscarAtivo(12L).nome());
    }

    @Test
    void deve_ocultar_produto_inativo_quando_buscar_por_id() {
        Produto produto = produtoAtivo();
        produto.setAtivo(false);
        when(produtoRepository.findById(12L)).thenReturn(Optional.of(produto));

        assertThrows(RecursoNaoEncontradoException.class, () -> produtoService.buscarAtivo(12L));
    }

    @Test
    void deve_lancar_nao_encontrado_quando_produto_nao_existir() {
        when(produtoRepository.findById(12L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> produtoService.buscarAtivo(12L));
    }

    private Produto produtoAtivo() {
        Categoria categoria = new Categoria();
        categoria.setId(4L);
        categoria.setNome("Clássicos");
        Produto produto = new Produto();
        produto.setId(12L);
        produto.setCategoria(categoria);
        produto.setNome("Chocolate");
        produto.setDescricao("Cupcake de chocolate");
        produto.setPrecoBase(new BigDecimal("12.00"));
        produto.setAtivo(true);
        return produto;
    }
}