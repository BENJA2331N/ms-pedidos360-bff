package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.CatalogClient;
import com.pedidos360.bff.dto.ProductRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/products")
public class CatalogProxyController {

    private final CatalogClient catalogClient;

    public CatalogProxyController(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    /**
     * Lista todos los productos activos del catálogo.
     * Permitido para todos los usuarios autenticados (Cliente, Operador, Admin).
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        return catalogClient.getAllProducts();
    }

    /**
     * Detalle de un producto por ID.
     * Permitido para todos los usuarios autenticados.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        return catalogClient.getProductById(id);
    }

    /**
     * Crea un nuevo producto.
     * Permitido únicamente para el rol ADMIN.
     */
    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        return catalogClient.createProduct(dto);
    }

    /**
     * Modifica los datos de un producto.
     * Permitido para roles ADMIN y OPERADOR.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                          @Valid @RequestBody ProductRequestDTO dto) {
        return catalogClient.updateProduct(id, dto);
    }

    /**
     * Eliminación lógica (soft-delete) de un producto.
     * Permitido únicamente para el rol ADMIN.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        return catalogClient.deleteProduct(id);
    }
}
