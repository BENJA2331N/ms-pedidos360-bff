package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cola. El nombre no puede estar vacío, superar 255 caracteres
 * ni usar el prefijo "amq." (reservado por RabbitMQ).
 */
public class QueueAdminRequestDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio y no puede estar vacío")
    @Size(max = 255, message = "El nombre de la cola no puede superar los 255 caracteres")
    @Pattern(regexp = "^(?!amq\\.).*$", message = "El nombre de la cola no puede comenzar con 'amq.' (prefijo reservado)")
    private String name;

    private boolean durable = true;

    public QueueAdminRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }
}
