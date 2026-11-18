package com.cupcakes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cupcakes.dto.ItemCarrinhoRequest;
import com.cupcakes.dto.PedidoRequest;
import com.cupcakes.model.FormaPagamento;
import com.cupcakes.model.OpcaoPersonalizacao;
import com.cupcakes.model.Perfil;
import com.cupcakes.model.Produto;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.model.TipoEntrega;
import com.cupcakes.model.TipoOpcao;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.OpcaoPersonalizacaoRepository;
import com.cupcakes.repository.ProdutoRepository;
import com.cupcakes.repository.UsuarioRepository;
import com.cupcakes.service.CarrinhoService;
import com.cupcakes.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CUPCAKES_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
class PostgresCompraExternalIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private OpcaoPersonalizacaoRepository opcaoRepository;

    @Autowired
    private CarrinhoService carrinhoService;

    @Autowired
    private PedidoService pedidoService;

    @DynamicPropertySource
    static void registrarPropriedadesPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("CUPCAKES_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> valorEnv("CUPCAKES_TEST_DATABASE_USERNAME", "cupcakes_test"));
        registry.add("spring.datasource.password", () -> valorEnv("CUPCAKES_TEST_DATABASE_PASSWORD", "cupcakes_test"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void deve_persistir_fluxo_de_compra_quando_usar_postgresql_externo() {
        String email = "integracao-" + UUID.randomUUID() + "@exemplo.com";
        Usuario usuario = usuarioRepository.save(novoUsuario(email));
        Produto produto = produtoRepository.findAll(PageRequest.of(0, 1)).getContent().getFirst();
        Set<Long> opcoesIds = opcoesObrigatorias();

        carrinhoService.adicionarItem(email, new ItemCarrinhoRequest(produto.getId(), 2, opcoesIds));
        var pedido = pedidoService.criarPedido(email,
                new PedidoRequest(TipoEntrega.RETIRADA, null, FormaPagamento.PIX));
        var pago = pedidoService.confirmarPagamento(pedido.id(), email);

        assertEquals(usuario.getId(), usuarioRepository.findByEmailIgnoreCase(email).orElseThrow().getId());
        assertEquals(StatusPedido.PAGO, pago.status());
        assertEquals(2, pago.itens().getFirst().quantidade());
        assertEquals(2, pago.historico().size());
        assertFalse(pago.itens().getFirst().opcoesDescricao().isBlank());
    }

    private static String valorEnv(String nome, String padrao) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) return padrao;
        return valor;
    }

    private Usuario novoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Teste de Integração");
        usuario.setEmail(email);
        usuario.setSenhaHash("hash-de-integracao");
        usuario.setPerfil(Perfil.CLIENTE);
        return usuario;
    }

    private Set<Long> opcoesObrigatorias() {
        return opcaoRepository.findAllByAtivoTrueOrderByTipoAscNomeAsc().stream()
                .filter(opcao -> tipoObrigatorio(opcao.getTipo()))
                .collect(Collectors.groupingBy(OpcaoPersonalizacao::getTipo))
                .values()
                .stream()
                .map(opcoes -> opcoes.getFirst().getId())
                .collect(Collectors.toSet());
    }

    private boolean tipoObrigatorio(TipoOpcao tipo) {
        return tipo == TipoOpcao.MASSA || tipo == TipoOpcao.RECHEIO || tipo == TipoOpcao.COBERTURA;
    }
}