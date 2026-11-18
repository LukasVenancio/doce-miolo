package com.cupcakes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cupcakes.config.SecurityConfig;
import com.cupcakes.dto.CadastroRequest;
import com.cupcakes.dto.UsuarioResponse;
import com.cupcakes.exception.GlobalExceptionHandler;
import com.cupcakes.exception.NegocioException;
import com.cupcakes.model.Perfil;
import com.cupcakes.security.RestAccessDeniedHandler;
import com.cupcakes.security.RestAuthenticationEntryPoint;
import com.cupcakes.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        AuthControllerWebTest.SecurityTestConfiguration.class
})
class AuthControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void deve_retornar_400_problem_detail_quando_dto_cadastro_for_invalido() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"errado\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deve_retornar_201_quando_cadastro_valido() throws Exception {
        CadastroRequest request = new CadastroRequest("Ana", "ana@exemplo.com", "senha-segura");
        when(authService.cadastrar(any(CadastroRequest.class)))
                .thenReturn(new UsuarioResponse(5L, "Ana", "ana@exemplo.com", Perfil.CLIENTE));

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void deve_retornar_409_problem_detail_quando_email_ja_estiver_cadastrado() throws Exception {
        CadastroRequest request = new CadastroRequest("Ana", "ana@exemplo.com", "senha-segura");
        when(authService.cadastrar(any(CadastroRequest.class)))
                .thenThrow(new NegocioException("Já existe uma conta com este e-mail."));

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @TestConfiguration
    static class SecurityTestConfiguration {

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
            return new JwtAuthenticationConverter();
        }
    }
}