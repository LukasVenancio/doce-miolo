package com.cupcakes.controller;

import com.cupcakes.dto.PedidoRequest;
import com.cupcakes.dto.PedidoResponse;
import com.cupcakes.dto.PedidoResumoResponse;
import com.cupcakes.service.PedidoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody PedidoRequest request
    ) {
        return pedidoService.criarPedido(jwt.getSubject(), request);
    }

    @PostMapping("/{id}/pagamento/confirmar")
    public PedidoResponse confirmarPagamento(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable @Positive Long id
    ) {
        return pedidoService.confirmarPagamento(id, jwt.getSubject());
    }

    @PostMapping("/{id}/cancelar")
    public PedidoResponse cancelar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable @Positive Long id
    ) {
        return pedidoService.cancelarPedido(id, jwt.getSubject());
    }

    @GetMapping
    public Page<PedidoResumoResponse> listar(
        @AuthenticationPrincipal Jwt jwt,
        @PageableDefault(size = 12, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return pedidoService.listarPedidos(jwt.getSubject(), pageable);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable @Positive Long id
    ) {
        return pedidoService.buscarPedido(id, jwt.getSubject());
    }
}