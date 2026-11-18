package com.cupcakes.dto;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoPedidoRequest(
        @NotBlank @Size(max = 160) String logradouro,
        @NotBlank @Size(max = 20) String numero,
        @Size(max = 100) String complemento,
        @NotBlank @Size(max = 100) String bairro,
        @NotBlank @Size(max = 100) String cidade,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String uf,
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}") String cep
) {

    public String snapshot() {
        return Stream.of(logradouro, numero, complemento, bairro, cidade, uf, cep)
                .filter(valor -> valor != null && !valor.isBlank())
                .collect(Collectors.joining(", "));
    }
}