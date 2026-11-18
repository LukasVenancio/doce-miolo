package com.cupcakes.controller;

import com.cupcakes.dto.CadastroRequest;
import com.cupcakes.dto.LoginRequest;
import com.cupcakes.dto.TokenResponse;
import com.cupcakes.dto.UsuarioResponse;
import com.cupcakes.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
        AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest request) {
        return authService.cadastrar(request);
    }

    @PostMapping("/login")
    public TokenResponse autenticar(@Valid @RequestBody LoginRequest request) {
        return authService.autenticar(request);
    }
}