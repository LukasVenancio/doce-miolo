package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.cupcakes.config.JwtProperties;
import com.cupcakes.dto.CadastroRequest;
import com.cupcakes.dto.LoginRequest;
import com.cupcakes.exception.NegocioException;
import com.cupcakes.model.Perfil;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;
    private JwtProperties jwtProperties;

    @BeforeEach
    void preparar() {
        jwtProperties = new JwtProperties();
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtTokenService, jwtProperties);
    }

    @Test
    void deve_cadastrar_cliente_com_email_normalizado_e_senha_hash() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@exemplo.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-segura")).thenReturn("hash-seguro");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(8L);
            return usuario;
        });

        var response = authService.cadastrar(new CadastroRequest(" Ana ", " ANA@EXEMPLO.COM ", "senha-segura"));

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals("ana@exemplo.com", response.email());
        assertEquals("Ana", usuarioCaptor.getValue().getNome());
        assertEquals("hash-seguro", usuarioCaptor.getValue().getSenhaHash());
        assertEquals(Perfil.CLIENTE, response.perfil());
    }

    @Test
    void deve_rejeitar_email_existente_quando_cadastrar() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@exemplo.com")).thenReturn(true);

        assertThrows(NegocioException.class, () -> authService.cadastrar(
                new CadastroRequest("Ana", "ana@exemplo.com", "senha-segura")));
    }

    @Test
    void deve_emitir_token_quando_credenciais_validas() {
        Usuario usuario = usuario("ana@exemplo.com", "hash-seguro");
        when(usuarioRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "hash-seguro")).thenReturn(true);
        when(jwtTokenService.emitir(usuario)).thenReturn("token-jwt");

        var response = authService.autenticar(new LoginRequest(" ANA@EXEMPLO.COM ", "senha-segura"));

        assertEquals("token-jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(jwtProperties.getExpirationSeconds(), response.expiresIn());
    }

    @Test
    void deve_rejeitar_credenciais_invalidas_quando_email_nao_existe() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.autenticar(new LoginRequest("ana@exemplo.com", "senha-segura")));
    }

    @Test
    void deve_rejeitar_credenciais_invalidas_quando_senha_nao_corresponde() {
        Usuario usuario = usuario("ana@exemplo.com", "hash-seguro");
        when(usuarioRepository.findByEmailIgnoreCase("ana@exemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-errada", "hash-seguro")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.autenticar(new LoginRequest("ana@exemplo.com", "senha-errada")));
    }

    private Usuario usuario(String email, String senhaHash) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenhaHash(senhaHash);
        usuario.setPerfil(Perfil.CLIENTE);
        return usuario;
    }
}