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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresCompraIntegrationTest {

    @Container
    private static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withEnv("POSTGRES_DB", "cupcakes_test")
            .withEnv("POSTGRES_USER", "cupcakes_test")
            .withEnv("POSTGRES_PASSWORD", "cupcakes_test")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2));

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
        registry.add("spring.datasource.url", PostgresCompraIntegrationTest::urlPostgres);
        registry.add("spring.datasource.username", () -> "cupcakes_test");
        registry.add("spring.datasource.password", () -> "cupcakes_test");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void deve_persistir_fluxo_de_compra_quando_usar_postgresql() {
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

    private static String urlPostgres() {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/cupcakes_test";
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