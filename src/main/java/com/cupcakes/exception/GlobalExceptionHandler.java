package com.cupcakes.exception;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ProblemDetail> handleRecursoNaoEncontrado(RecursoNaoEncontradoException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    ResponseEntity<ProblemDetail> handleTransicaoInvalida(TransicaoInvalidaException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ProblemDetail> handleNegocio(NegocioException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ValidacaoNegocioException.class)
    ResponseEntity<ProblemDetail> handleValidacaoNegocio(ValidacaoNegocioException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> handleCredenciaisInvalidas() {
        return problem(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleConflitoDeDados() {
        return problem(HttpStatus.CONFLICT, "O registro já existe ou viola uma restrição.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleArgumentoInvalido(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleViolacaoDeRestricao(ConstraintViolationException exception) {
        String detail = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> handleValidacaoDeParametros() {
        return problem(HttpStatus.BAD_REQUEST, "Os parâmetros da requisição são inválidos.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleMensagemInvalida() {
        return problem(HttpStatus.BAD_REQUEST, "A requisição contém dados inválidos.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleErroInterno() {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problem);
    }
}