package com.cupcakes.dto;

import com.cupcakes.model.Perfil;
import com.cupcakes.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }
}