package com.cupcakes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import com.cupcakes.dto.ItemCarrinhoRequest;
import com.cupcakes.model.Perfil;
import com.cupcakes.model.Produto;
import com.cupcakes.model.TipoOpcao;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.OpcaoPersonalizacaoRepository;
import com.cupcakes.repository.ProdutoRepository;
import com.cupcakes.repository.UsuarioRepository;
import com.cupcakes.service.CarrinhoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class CarrinhoFetchIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private OpcaoPersonalizacaoRepository opcaoRepository;

    @Autowired
    private CarrinhoService carrinhoService;

    @Test
    void deve_retornar_um_item_com_tres_opcoes_quando_recarregar_carrinho() {
        String email = "carrinho-fetch@exemplo.com";
        Usuario usuario = usuarioRepository.save(novoUsuario(email));
        Produto produto = produtoRepository.findAll(PageRequest.of(0, 1)).getContent().getFirst();
        Set<Long> opcoesIds = opcoesPorTipo(TipoOpcao.MASSA, TipoOpcao.RECHEIO, TipoOpcao.COBERTURA);

        carrinhoService.adicionarItem(email, new ItemCarrinhoRequest(produto.getId(), 1, opcoesIds));
        var carrinho = carrinhoService.buscar(email);

        assertEquals(1, carrinho.itens().size());
        assertEquals(3, carrinho.itens().getFirst().opcoes().size());
    }

    private Usuario novoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste Carrinho");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(Perfil.CLIENTE);
        return usuario;
    }

    private Set<Long> opcoesPorTipo(TipoOpcao... tipos) {
        return opcaoRepository.findAllByAtivoTrueOrderByTipoAscNomeAsc().stream()
                .filter(opcao -> java.util.Arrays.asList(tipos).contains(opcao.getTipo()))
                .collect(java.util.stream.Collectors.groupingBy(com.cupcakes.model.OpcaoPersonalizacao::getTipo))
                .values()
                .stream()
                .map(opcoes -> opcoes.getFirst().getId())
                .collect(java.util.stream.Collectors.toSet());
    }
}
