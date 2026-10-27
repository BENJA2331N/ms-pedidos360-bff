package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ExchangeAdminRequestDTO {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
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
