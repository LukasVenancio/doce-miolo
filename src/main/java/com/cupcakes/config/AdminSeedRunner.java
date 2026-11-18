package com.cupcakes.config;

import com.cupcakes.model.Perfil;
import com.cupcakes.model.Usuario;
import com.cupcakes.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeedRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminSeedRunner(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        @Value("${ADMIN_EMAIL:}") String email,
        @Value("${ADMIN_PASSWORD:}") String password
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank()) {
            return;
        }
        if (password.isBlank()) {
            return;
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        usuarioRepository.save(novoAdministrador());
    }

    private Usuario novoAdministrador() {
        Usuario administrador = new Usuario();
        administrador.setNome("Administrador");
        administrador.setEmail(email);
        administrador.setSenhaHash(passwordEncoder.encode(password));
        administrador.setPerfil(Perfil.ADMIN);
        return administrador;
    }
}