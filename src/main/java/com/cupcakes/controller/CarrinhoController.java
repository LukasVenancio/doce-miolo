package com.cupcakes.controller;

import com.cupcakes.dto.CarrinhoResponse;
import com.cupcakes.dto.ItemCarrinhoRequest;
import com.cupcakes.dto.QuantidadeRequest;
import com.cupcakes.service.CarrinhoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    @GetMapping
    public CarrinhoResponse buscar(@AuthenticationPrincipal Jwt jwt) {
        return carrinhoService.buscar(jwt.getSubject());
    }

    @PostMapping("/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public CarrinhoResponse adicionarItem(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ItemCarrinhoRequest request
    ) {
        return carrinhoService.adicionarItem(jwt.getSubject(), request);
    }

    @PutMapping("/itens/{id}")
    public CarrinhoResponse atualizarQuantidade(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable @Positive Long id,
        @Valid @RequestBody QuantidadeRequest request
    ) {
        return carrinhoService.atualizarQuantidade(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/itens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerItem(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable @Positive Long id
    ) {
        carrinhoService.removerItem(jwt.getSubject(), id);
    }
}