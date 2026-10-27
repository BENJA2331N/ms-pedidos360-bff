package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un exchange. El tipo debe ser direct, topic o fanout; el nombre no puede
 * estar vacío, superar 255 caracteres ni usar el prefijo "amq." (reservado por RabbitMQ).
 */
public class ExchangeAdminRequestDTO {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(max = 255, message = "El nombre del exchange no puede superar los 255 caracteres")
    @Pattern(regexp = "^(?!amq\\.).*$", message = "El nombre del exchange no puede comenzar con 'amq.' (prefijo reservado)")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(regexp = "^(direct|topic|fanout)$", message = "El tipo de exchange debe ser direct, topic o fanout")
    private String type = "direct";

    public ExchangeAdminRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
