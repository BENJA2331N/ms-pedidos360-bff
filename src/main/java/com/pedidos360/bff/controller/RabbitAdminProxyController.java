package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.RabbitAdminClient;
import com.pedidos360.bff.dto.BindingAdminRequestDTO;
import com.pedidos360.bff.dto.ExchangeAdminRequestDTO;
import com.pedidos360.bff.dto.QueueAdminRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Proxy del BFF hacia ms-pedidos360-rabbit-admin. Valida la entrada y delega en {@link RabbitAdminClient}.
 * Acceso restringido al rol ADMIN (ver SecurityConfig).
 */
@Validated
@RestController
@RequestMapping("/api/rabbit-admin")
public class RabbitAdminProxyController {

    private final RabbitAdminClient rabbitAdminClient;

    public RabbitAdminProxyController(RabbitAdminClient rabbitAdminClient) {
        this.rabbitAdminClient = rabbitAdminClient;
    }

    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> createQueue(@Valid @RequestBody QueueAdminRequestDTO request) {
        return rabbitAdminClient.createQueue(request);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Map<String, String>> deleteQueue(@PathVariable("name") @NotBlank String name) {
        return rabbitAdminClient.deleteQueue(name);
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> createExchange(@Valid @RequestBody ExchangeAdminRequestDTO request) {
        return rabbitAdminClient.createExchange(request);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Map<String, String>> deleteExchange(@PathVariable("name") @NotBlank String name) {
        return rabbitAdminClient.deleteExchange(name);
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> createBinding(@Valid @RequestBody BindingAdminRequestDTO request) {
        return rabbitAdminClient.createBinding(request);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, String>> deleteBinding(
            @RequestParam("queueName") @NotBlank(message = "El nombre de la cola no puede estar vacío") String queueName,
            @RequestParam("exchangeName") @NotBlank(message = "El nombre del exchange no puede estar vacío") String exchangeName,
            @RequestParam("routingKey") @NotBlank(message = "El routingKey no puede estar vacío") String routingKey) {
        return rabbitAdminClient.deleteBinding(queueName, exchangeName, routingKey);
    }
}
