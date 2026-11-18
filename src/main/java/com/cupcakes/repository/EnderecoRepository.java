package com.cupcakes.repository;

import java.util.List;

import com.cupcakes.model.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    List<Endereco> findAllByUsuarioIdOrderById(Long usuarioId);
}