package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para enlazar (bind) una cola con un exchange mediante un routingKey.
 */
public class BindingAdminRequestDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(max = 255, message = "El nombre de la cola no puede superar los 255 caracteres")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(max = 255, message = "El nombre del exchange no puede superar los 255 caracteres")
    private String exchangeName;

    @NotBlank(message = "El routingKey no puede estar vacío")
    @Size(max = 255, message = "El routingKey no puede superar los 255 caracteres")
    private String routingKey;

    public BindingAdminRequestDTO() {}

    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
}
