package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.OrdersClient;
import com.pedidos360.bff.dto.OrderRequestDTO;
import com.pedidos360.bff.dto.StatusUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderProxyController {

    private final OrdersClient ordersClient;

    public OrderProxyController(OrdersClient ordersClient) {
        this.ordersClient = ordersClient;
    }

    /**
     * Obtiene únicamente los pedidos del cliente autenticado.
     * Extrae el identificador del cliente desde el JWT (oid o sub).
     */
    @GetMapping("/mine")
    public ResponseEntity<?> misPedidos(@AuthenticationPrincipal Jwt jwt) {
        String clienteId = resolveClienteId(jwt);
        return ordersClient.getMisPedidos(clienteId);
    }

    /**
     * Lista todos los pedidos del sistema (solo Operador / Admin).
     */
    @GetMapping
    public ResponseEntity<?> todosLosPedidos() {
        return ordersClient.getTodosLosPedidos();
    }

    /**
     * Consulta el detalle de un pedido por ID (solo Operador / Admin).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> pedidoPorId(@PathVariable Long id) {
        return ordersClient.getPedidoPorId(id);
    }

    /**
     * Crea un nuevo pedido asociado al cliente autenticado.
     */
    @PostMapping
    public ResponseEntity<?> crearPedido(@Valid @RequestBody OrderRequestDTO dto,
                                        @AuthenticationPrincipal Jwt jwt) {
        String clienteId = resolveClienteId(jwt);
        return ordersClient.crear(dto, clienteId);
    }

    /**
     * Cambia el estado de un pedido según el flujo de la máquina de estados.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateDTO dto) {
        return ordersClient.cambiarEstado(id, dto);
    }

    /**
     * Extrae el identificador único del usuario desde el JWT.
     * Prioriza el claim 'oid' (Object ID de Microsoft Entra ID) y luego 'sub' (Subject).
     */
    private String resolveClienteId(Jwt jwt) {
        if (jwt == null) {
            return "anonymous";
        }
        String oid = jwt.getClaimAsString("oid");
        if (oid != null && !oid.isBlank()) {
            return oid;
        }
        String sub = jwt.getSubject();
        if (sub != null && !sub.isBlank()) {
            return sub;
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return "desconocido";
    }
}
