package com.cupcakes.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deve_retornar_404_quando_recurso_nao_existir() {
        var response = handler.handleRecursoNaoEncontrado(new RecursoNaoEncontradoException("Ausente"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ausente", response.getBody().getDetail());
    }

    @Test
    void deve_retornar_409_quando_regra_de_negocio_entrar_em_conflito() {
        var response = handler.handleNegocio(new NegocioException("Conflito"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void deve_retornar_400_quando_regra_de_validacao_falhar() {
        var response = handler.handleValidacaoNegocio(new ValidacaoNegocioException("Inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deve_retornar_401_com_mensagem_generica_quando_credenciais_falharem() {
        var response = handler.handleCredenciaisInvalidas();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("E-mail ou senha inválidos.", response.getBody().getDetail());
    }

    @Test
    void deve_retornar_500_sem_detalhes_internos_quando_ocorrer_erro_inesperado() {
        var response = handler.handleErroInterno();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ocorreu um erro interno.", response.getBody().getDetail());
    }
}