package com.pedidos360.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(@Value("${catalog.service.url:http://localhost:8081}") String catalogUrl,
                         RestClient.Builder builder) {
        this.restClient = builder.baseUrl(catalogUrl).build();
    }

    /**
     * Obtiene el listado completo de productos activos.
     */
    public ResponseEntity<Object> getAllProducts() {
        return restClient.get()
                .uri("/api/catalog/products")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Obtiene un producto por su identificador único.
     */
    public ResponseEntity<Object> getProductById(Long id) {
        return restClient.get()
                .uri("/api/catalog/products/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Crea un nuevo producto en el catálogo (solo Admin).
     */
    public ResponseEntity<Object> createProduct(Object dto) {
        return restClient.post()
                .uri("/api/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Modifica los datos de un producto (Admin u Operador).
     */
    public ResponseEntity<Object> updateProduct(Long id, Object dto) {
        return restClient.put()
                .uri("/api/catalog/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toEntity(Object.class);
    }

    /**
     * Elimina lógicamente un producto del catálogo (solo Admin).
     */
    public ResponseEntity<Void> deleteProduct(Long id) {
        return restClient.delete()
                .uri("/api/catalog/products/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
