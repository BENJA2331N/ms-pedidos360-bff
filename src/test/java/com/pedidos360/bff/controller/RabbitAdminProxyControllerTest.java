package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.RabbitAdminClient;
import com.pedidos360.bff.config.RoleConverter;
import com.pedidos360.bff.config.SecurityConfig;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RabbitAdminProxyController.class)
@Import({SecurityConfig.class, RoleConverter.class})
class RabbitAdminProxyControllerTest {

    private static final String QUEUE_JSON = "{\"name\":\"cola.test\",\"durable\":true}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private RabbitAdminClient rabbitAdminClient;

    @Test
    @DisplayName("Sin token -> 401 Unauthorized")
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/rabbit-admin/queues")
                        .contentType(MediaType.APPLICATION_JSON).content(QUEUE_JSON))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(rabbitAdminClient);
    }

    @Test
    @DisplayName("Rol sin permisos (OPERADOR) -> 403 Forbidden")
    void operadorReturns403() throws Exception {
        mockMvc.perform(post("/api/rabbit-admin/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR")))
                        .contentType(MediaType.APPLICATION_JSON).content(QUEUE_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(rabbitAdminClient);
    }

    @Test
    @DisplayName("ADMIN crea una cola -> se delega en el cliente y retorna 201")
    void adminCreatesQueue() throws Exception {
        when(rabbitAdminClient.createQueue(any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensaje", "ok")));

        mockMvc.perform(post("/api/rabbit-admin/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content(QUEUE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("ADMIN con nombre de cola vacío -> 400 sin llamar al microservicio")
    void adminBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/rabbit-admin/queues")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(rabbitAdminClient);
    }

    @Test
    @DisplayName("ADMIN elimina un binding por query params -> se delega en el cliente")
    void adminDeletesBinding() throws Exception {
        when(rabbitAdminClient.deleteBinding("q", "e", "rk"))
                .thenReturn(ResponseEntity.ok(Map.of("mensaje", "ok")));

        mockMvc.perform(delete("/api/rabbit-admin/bindings")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("queueName", "q").param("exchangeName", "e").param("routingKey", "rk"))
                .andExpect(status().isOk());
        verify(rabbitAdminClient).deleteBinding("q", "e", "rk");
    }
}
