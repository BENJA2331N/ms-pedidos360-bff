package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;

public class BindingAdminRequestDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    private String exchangeName;

    @NotBlank(message = "El routingKey no puede estar vacío")
    private String routingKey;

    public BindingAdminRequestDTO() {}

    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }
    public String getExchangeName() { return exchangeName; }
    public void setExchangeName(String exchangeName) { this.exchangeName = exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
}
