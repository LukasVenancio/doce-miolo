package com.cupcakes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.cupcakes.config.JwtProperties;
import com.cupcakes.model.Perfil;
import com.cupcakes.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Test
    void deve_emitir_token_com_perfil_e_expiracao_quando_gerar() {
        JwtProperties properties = new JwtProperties();
        properties.setExpirationMinutes(30);
        JwtTokenService service = new JwtTokenService(jwtEncoder, properties);
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@exemplo.com");
        usuario.setPerfil(Perfil.CLIENTE);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("token-assinado").header("alg", "HS256")
                        .subject("ana@exemplo.com").claim("perfil", "CLIENTE").build());

        assertEquals("token-assinado", service.emitir(usuario));

        ArgumentCaptor<JwtEncoderParameters> parametros = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(parametros.capture());
        var claims = parametros.getValue().getClaims();
        assertEquals("ana@exemplo.com", claims.getSubject());
        assertEquals("CLIENTE", claims.getClaim("perfil"));
        assertEquals(Duration.ofMinutes(30), Duration.between(claims.getIssuedAt(), claims.getExpiresAt()));
    }
}