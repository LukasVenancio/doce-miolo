package com.cupcakes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deve_servir_pagina_e_assets_quando_acessar_sem_autenticacao() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
            .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/js/catalogo-page.js"))
                .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("carregarProdutos")));
    }

    @Test
    void deve_retornar_401_problem_detail_quando_carrinho_for_acessado_sem_token() throws Exception {
        mockMvc.perform(get("/api/carrinho"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deve_retornar_403_problem_detail_quando_cliente_acessar_admin() throws Exception {
        mockMvc.perform(get("/api/admin/produtos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deve_retornar_problem_detail_quando_cadastro_for_invalido() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","email":"invalido","senha":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deve_cadastrar_autenticar_e_consultar_carrinho_com_jwt() throws Exception {
        String email = "cliente-" + UUID.randomUUID() + "@exemplo.com";
        cadastrarCliente(email);
        String token = autenticarCliente(email);

        mockMvc.perform(get("/api/carrinho").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.subtotal").value(0));
    }

    @Test
    void deve_listar_catalogo_publico_quando_sem_token() throws Exception {
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

            @Test
            void deve_listar_categorias_e_opcoes_sem_autenticacao() throws Exception {
            mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").isNotEmpty());

            mockMvc.perform(get("/api/opcoes-personalizacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").isNotEmpty());
            }

    @Test
    void deve_permitir_listagem_admin_quando_token_for_admin() throws Exception {
        mockMvc.perform(get("/api/admin/produtos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void deve_autorizar_rota_cliente_e_isolar_pedidos_por_usuario() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .with(jwt().jwt(token -> token.subject("ausente@exemplo.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deve_retornar_404_problem_detail_quando_admin_atualizar_pedido_ausente() throws Exception {
        mockMvc.perform(patch("/api/admin/pedidos/999999/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_PREPARO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deve_retornar_401_generico_quando_login_nao_encontrar_email() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "ausente@exemplo.com", "senha", "senha-segura"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    void deve_retornar_409_problem_detail_quando_email_ja_existir() throws Exception {
        String email = "duplicado-" + UUID.randomUUID() + "@exemplo.com";
        cadastrarCliente(email);

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Ana", "email", email.toUpperCase(), "senha", "senha-segura"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    private void cadastrarCliente(String email) throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nome", "Cliente Teste", "email", email, "senha", "senha-segura"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    private String autenticarCliente(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", "senha-segura"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode corpo = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return corpo.get("accessToken").asText();
    }
}