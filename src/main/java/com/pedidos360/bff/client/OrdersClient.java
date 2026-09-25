package com.pedidos360.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrdersClient {

    private final RestClient restClient;

    public OrdersClient(@Value("${orders.service.url:http://localhost:8082}") String ordersUrl,
                        RestClient.Builder builder) {
        this.restClient = builder.baseUrl(ordersUrl).build();
    }

    /**
     * Obtiene los pedidos del cliente autenticado.
     */
    public ResponseEntity<Object> getMisPedidos(String clienteId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/mine")
                        .queryParam("clienteId", clienteId)
                        .build())
                .header("X-Cliente-Id", clienteId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Obtiene todos los pedidos del sistema (solo Operador / Admin).
     */
    public ResponseEntity<Object> getTodosLosPedidos() {
        return restClient.get()
                .uri("/api/orders")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Obtiene el detalle de un pedido por su identificador.
     */
    public ResponseEntity<Object> getPedidoPorId(Long id) {
        return restClient.get()
                .uri("/api/orders/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Crea un nuevo pedido para el cliente autenticado.
     */
    public ResponseEntity<Object> crear(Object dto, String clienteId) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders")
                        .queryParam("clienteId", clienteId)
                        .build())
                .header("X-Cliente-Id", clienteId)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Actualiza el estado de un pedido (Operador / Admin).
     */
    public ResponseEntity<Object> cambiarEstado(Long id, Object dto) {
        return restClient.patch()
                .uri("/api/orders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toEntity(Object.class);
    }
}
