package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;

public class QueueAdminRequestDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio y no puede estar vacío")
    private String name;

    private boolean durable = true;

    public QueueAdminRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isDurable() { return durable; }
    public void setDurable(boolean durable) { this.durable = durable; }
}
