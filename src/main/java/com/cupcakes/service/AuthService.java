package com.cupcakes.service;

import java.util.Locale;

import com.cupcakes.config.JwtProperties;
import com.cupcakes.dto.CadastroRequest;
import com.cupcakes.dto.LoginRequest;
import com.cupcakes.dto.TokenResponse;
import com.cupcakes.dto.UsuarioResponse;
import com.cupcakes.exception.NegocioException;
import com.cupcakes.model.Perfil;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenService jwtTokenService,
        JwtProperties jwtProperties
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UsuarioResponse cadastrar(CadastroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new NegocioException("Já existe uma conta com este e-mail.");
        }
        return UsuarioResponse.de(usuarioRepository.save(novoCliente(request, email)));
    }

    @Transactional(readOnly = true)
    public TokenResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(request.email()))
                .orElseThrow(this::credenciaisInvalidas);
        validarSenha(request.senha(), usuario);
        return new TokenResponse(
                jwtTokenService.emitir(usuario), "Bearer", jwtProperties.getExpirationSeconds());
    }

    private Usuario novoCliente(CadastroRequest request, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().strip());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(Perfil.CLIENTE);
        return usuario;
    }

    private String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private void validarSenha(String senha, Usuario usuario) {
        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw credenciaisInvalidas();
        }
    }

    private BadCredentialsException credenciaisInvalidas() {
        return new BadCredentialsException("Credenciais inválidas.");
    }
}