package com.cupcakes.controller;

import com.cupcakes.dto.PedidoResponse;
import com.cupcakes.dto.PedidoResumoResponse;
import com.cupcakes.dto.StatusRequest;
import com.cupcakes.model.StatusPedido;
import com.cupcakes.service.AdminPedidoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/pedidos")
public class AdminPedidoController {

    private final AdminPedidoService adminPedidoService;

    public AdminPedidoController(AdminPedidoService adminPedidoService) {
        this.adminPedidoService = adminPedidoService;
    }

    @GetMapping
    public Page<PedidoResumoResponse> listar(
        @RequestParam(required = false) StatusPedido status,
        @PageableDefault(size = 12, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return adminPedidoService.listar(status, pageable);
    }

    @PatchMapping("/{id}/status")
    public PedidoResponse atualizarStatus(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable @Positive Long id,
        @Valid @RequestBody StatusRequest request
    ) {
        return adminPedidoService.atualizarStatus(id, request.status(), jwt.getSubject());
    }
}