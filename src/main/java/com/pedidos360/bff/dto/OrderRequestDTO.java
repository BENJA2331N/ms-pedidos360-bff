package com.pedidos360.bff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class OrderRequestDTO {

    @NotEmpty(message = "El pedido debe contener al menos un producto")
    @Valid
    private List<OrderItemRequestDTO> items = new ArrayList<>();

    public OrderRequestDTO() {
    }

    public OrderRequestDTO(List<OrderItemRequestDTO> items) {
        this.items = items;
    }

    public List<OrderItemRequestDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequestDTO> items) {
        this.items = items;
    }
}
