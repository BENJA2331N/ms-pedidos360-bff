package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Datos para crear una cola. El nombre no puede estar vacío, superar 255 caracteres
 * ni usar el prefijo "amq." (reservado por RabbitMQ). Permite opcionalmente
 * asociar un Dead Letter Exchange (DLX) y routing key.
 */
public class QueueAdminRequestDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio y no puede estar vacío")
    @Size(max = 255, message = "El nombre de la cola no puede superar los 255 caracteres")
    @Pattern(regexp = "^(?!amq\\.).*$", message = "El nombre de la cola no puede comenzar con 'amq.' (prefijo reservado)")
    private String name;

    private boolean durable = true;

    @Size(max = 255, message = "El nombre del deadLetterExchange no puede superar los 255 caracteres")
    private String deadLetterExchange;

    @Size(max = 255, message = "El deadLetterRoutingKey no puede superar los 255 caracteres")
    private String deadLetterRoutingKey;

    private Map<String, Object> arguments;

    public QueueAdminRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }
    public String getDeadLetterExchange() { return deadLetterExchange; }
    public void setDeadLetterExchange(String deadLetterExchange) { this.deadLetterExchange = deadLetterExchange; }
    public String getDeadLetterRoutingKey() { return deadLetterRoutingKey; }
    public void setDeadLetterRoutingKey(String deadLetterRoutingKey) { this.deadLetterRoutingKey = deadLetterRoutingKey; }
    public Map<String, Object> getArguments() { return arguments; }
    public void setArguments(Map<String, Object> arguments) { this.arguments = arguments; }
}
