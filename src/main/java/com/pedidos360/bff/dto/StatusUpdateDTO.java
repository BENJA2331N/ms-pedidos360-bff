package com.pedidos360.bff.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class StatusUpdateDTO {

    @NotBlank(message = "El estado no puede estar vacío")
    @JsonAlias({"nuevoEstado", "status"})
    private String estado;

    public StatusUpdateDTO() {
    }

    public StatusUpdateDTO(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getNuevoEstado() {
        return estado;
    }

    public void setNuevoEstado(String nuevoEstado) {
        this.estado = nuevoEstado;
    }
}
