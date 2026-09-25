package com.pedidos360.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.bff.client.CatalogClient;
import com.pedidos360.bff.client.OrdersClient;
import com.pedidos360.bff.config.RoleConverter;
import com.pedidos360.bff.config.SecurityConfig;
import com.pedidos360.bff.dto.OrderItemRequestDTO;
import com.pedidos360.bff.dto.OrderRequestDTO;
import com.pedidos360.bff.dto.ProductRequestDTO;
import com.pedidos360.bff.dto.StatusUpdateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrderProxyController.class, CatalogProxyController.class})
@Import({SecurityConfig.class, RoleConverter.class})
class SecurityAndProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private OrdersClient ordersClient;

    @MockBean
    private CatalogClient catalogClient;

    // =========================================================================
    // 1. Pruebas de Autenticación General (401 Unauthorized)
    // =========================================================================

    @Test
    @DisplayName("Petición sin token debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // =========================================================================
    // 2. Pruebas de Autorización Catálogo (/api/catalog/products)
    // =========================================================================

    @Test
    @DisplayName("Cualquier rol autenticado (CLIENTE) puede consultar el catálogo GET /api/catalog/products")
    void clienteCanReadCatalog() throws Exception {
        when(catalogClient.getAllProducts()).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/api/catalog/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CLIENTE no puede crear productos POST /api/catalog/products -> 403 Forbidden")
    void clienteCannotCreateProduct() throws Exception {
        ProductRequestDTO dto = new ProductRequestDTO("Teclado", "Mecánico", new BigDecimal("49.990"), 10);

        mockMvc.perform(post("/api/catalog/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("OPERADOR no puede crear productos POST /api/catalog/products -> 403 Forbidden")
    void operadorCannotCreateProduct() throws Exception {
        ProductRequestDTO dto = new ProductRequestDTO("Teclado", "Mecánico", new BigDecimal("49.990"), 10);

        mockMvc.perform(post("/api/catalog/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN sí puede crear productos POST /api/catalog/products -> 201 Created")
    void adminCanCreateProduct() throws Exception {
        ProductRequestDTO dto = new ProductRequestDTO("Teclado", "Mecánico", new BigDecimal("49.990"), 10);
        when(catalogClient.createProduct(any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(dto));

        mockMvc.perform(post("/api/catalog/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("OPERADOR puede editar productos PUT /api/catalog/products/{id} -> 200 OK")
    void operadorCanUpdateProduct() throws Exception {
        ProductRequestDTO dto = new ProductRequestDTO("Teclado", "Mecánico RGB", new BigDecimal("59.990"), 15);
        when(catalogClient.updateProduct(eq(1L), any())).thenReturn(ResponseEntity.ok(dto));

        mockMvc.perform(put("/api/catalog/products/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OPERADOR no puede eliminar productos DELETE /api/catalog/products/{id} -> 403 Forbidden")
    void operadorCannotDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/catalog/products/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN sí puede eliminar productos DELETE /api/catalog/products/{id} -> 204 No Content")
    void adminCanDeleteProduct() throws Exception {
        when(catalogClient.deleteProduct(1L)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/api/catalog/products/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // 3. Pruebas de Autorización Pedidos (/api/orders)
    // =========================================================================

    @Test
    @DisplayName("CLIENTE puede consultar sus propios pedidos GET /api/orders/mine -> 200 OK")
    void clienteCanGetOwnOrders() throws Exception {
        when(ordersClient.getMisPedidos(anyString())).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/api/orders/mine")
                        .with(jwt()
                                .jwt(builder -> builder.subject("cli-12345").claim("oid", "cli-oid-999"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CLIENTE no puede listar todos los pedidos GET /api/orders -> 403 Forbidden")
    void clienteCannotGetAllOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OPERADOR sí puede listar todos los pedidos GET /api/orders -> 200 OK")
    void operadorCanGetAllOrders() throws Exception {
        when(ordersClient.getTodosLosPedidos()).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/api/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CLIENTE puede crear pedidos POST /api/orders -> 200 OK")
    void clienteCanCreateOrder() throws Exception {
        OrderRequestDTO dto = new OrderRequestDTO(List.of(
                new OrderItemRequestDTO(1L, 2, new BigDecimal("15.50"))
        ));

        when(ordersClient.crear(any(), eq("cli-oid-777"))).thenReturn(ResponseEntity.ok(Map.of("id", 100)));

        mockMvc.perform(post("/api/orders")
                        .with(jwt()
                                .jwt(builder -> builder.claim("oid", "cli-oid-777"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CLIENTE no puede cambiar estado de pedido PATCH /api/orders/{id}/status -> 403 Forbidden")
    void clienteCannotUpdateOrderStatus() throws Exception {
        StatusUpdateDTO dto = new StatusUpdateDTO("ACEPTADO");

        mockMvc.perform(patch("/api/orders/1/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OPERADOR sí puede cambiar estado de pedido PATCH /api/orders/{id}/status -> 200 OK")
    void operadorCanUpdateOrderStatus() throws Exception {
        StatusUpdateDTO dto = new StatusUpdateDTO("ACEPTADO");
        when(ordersClient.cambiarEstado(eq(1L), any())).thenReturn(ResponseEntity.ok(Map.of("status", "ACEPTADO")));

        mockMvc.perform(patch("/api/orders/1/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
