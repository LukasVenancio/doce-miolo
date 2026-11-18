package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.cupcakes.dto.ProdutoRequest;
import com.cupcakes.exception.RecursoNaoEncontradoException;
import com.cupcakes.model.Categoria;
import com.cupcakes.model.Produto;
import com.cupcakes.repository.CategoriaRepository;
import com.cupcakes.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    private AdminProdutoService service;
    private Produto produto;

    @BeforeEach
    void preparar() {
        service = new AdminProdutoService(produtoRepository, categoriaRepository);
        produto = new Produto();
        produto.setId(7L);
        produto.setAtivo(true);
    }

    @Test
    void deve_inativar_sem_excluir_quando_admin_inativar_produto() {
        when(produtoRepository.findById(7L)).thenReturn(Optional.of(produto));

        service.inativar(7L);

        assertFalse(produto.isAtivo());
        verify(produtoRepository, never()).deleteById(7L);
    }

    @Test
    void deve_criar_produto_quando_categoria_existir() {
        Categoria categoria = categoria();
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> {
            Produto salvo = invocation.getArgument(0);
            salvo.setId(9L);
            return salvo;
        });

        var response = service.criar(requisicao());

        assertEquals(9L, response.id());
        assertEquals("Cupcake Teste", response.nome());
    }

    @Test
    void deve_atualizar_produto_quando_produto_e_categoria_existirem() {
        Categoria categoria = categoria();
        when(produtoRepository.findById(7L)).thenReturn(Optional.of(produto));
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(categoria));
        when(produtoRepository.save(produto)).thenReturn(produto);

        var response = service.atualizar(7L, requisicao());

        assertEquals("Cupcake Teste", response.nome());
        assertEquals(2L, response.categoriaId());
    }

    @Test
    void deve_listar_produtos_quando_admin_consultar_catalogo() {
        Categoria categoria = categoria();
        produto.setCategoria(categoria);
        produto.setNome("Cupcake Teste");
        var pageable = PageRequest.of(0, 10);
        when(produtoRepository.findAllByOrderByNomeAsc(pageable))
                .thenReturn(new PageImpl<>(List.of(produto), pageable, 1));

        var pagina = service.listar(pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Cupcake Teste", pagina.getContent().getFirst().nome());
    }

    @Test
    void deve_buscar_produto_quando_admin_consultar_id() {
        Categoria categoria = categoria();
        produto.setCategoria(categoria);
        produto.setNome("Cupcake Teste");
        when(produtoRepository.findById(7L)).thenReturn(Optional.of(produto));

        assertEquals("Cupcake Teste", service.buscar(7L).nome());
    }

    @Test
    void deve_lancar_nao_encontrado_quando_categoria_nao_existir() {
        when(categoriaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.criar(requisicao()));
    }

    @Test
    void deve_lancar_nao_encontrado_quando_produto_nao_existir() {
        when(produtoRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.inativar(7L));
    }

    @Test
    void deve_rejeitar_edicao_quando_produto_nao_existir() {
        when(produtoRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.atualizar(7L, requisicao()));
    }

    private Categoria categoria() {
        Categoria categoria = new Categoria();
        categoria.setId(2L);
        categoria.setNome("Especiais");
        return categoria;
    }

    private ProdutoRequest requisicao() {
        return new ProdutoRequest(2L, " Cupcake Teste ", " Descrição ",
                new BigDecimal("13.50"), null, true);
    }
}