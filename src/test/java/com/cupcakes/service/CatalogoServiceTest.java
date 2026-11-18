package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import com.cupcakes.model.Categoria;
import com.cupcakes.model.OpcaoPersonalizacao;
import com.cupcakes.model.TipoOpcao;
import com.cupcakes.repository.CategoriaRepository;
import com.cupcakes.repository.OpcaoPersonalizacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private OpcaoPersonalizacaoRepository opcaoRepository;

    private CatalogoService catalogoService;

    @BeforeEach
    void preparar() {
        catalogoService = new CatalogoService(categoriaRepository, opcaoRepository);
    }

    @Test
    void deve_listar_categorias_ordenadas_quando_consultar_catalogo() {
        Categoria categoria = new Categoria();
        categoria.setId(3L);
        categoria.setNome("Clássicos");
        when(categoriaRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(categoria));

        var categorias = catalogoService.listarCategorias();

        assertEquals(1, categorias.size());
        assertEquals("Clássicos", categorias.getFirst().nome());
    }

    @Test
    void deve_listar_opcoes_ativas_quando_consultar_personalizacoes() {
        OpcaoPersonalizacao opcao = new OpcaoPersonalizacao();
        opcao.setId(4L);
        opcao.setTipo(TipoOpcao.EXTRA);
        opcao.setNome("Granulado");
        when(opcaoRepository.findAllByAtivoTrueOrderByTipoAscNomeAsc()).thenReturn(List.of(opcao));

        var opcoes = catalogoService.listarOpcoesAtivas();

        assertEquals(1, opcoes.size());
        assertEquals("Granulado", opcoes.getFirst().nome());
    }
}