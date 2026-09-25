package com.pedidos360.bff.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura AccessDeniedException -> 403 con mensaje claro.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Acceso denegado: No cuenta con los privilegios o roles necesarios para realizar esta acción. " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Captura errores de autenticación y tokens inválidos o malformados -> 401 Unauthorized.
     */
    @ExceptionHandler({AuthenticationException.class, BadJwtException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Token de autenticación inválido, expirado o no autorizado: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Propaga códigos de error (4xx y 5xx) recibidos desde los microservicios downstream (Orders o Catalog).
     */
    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<?> handleHttpStatusCodeException(HttpStatusCodeException ex) {
        HttpHeaders headers = new HttpHeaders();
        MediaType contentType = ex.getResponseHeaders() != null && ex.getResponseHeaders().getContentType() != null
                ? ex.getResponseHeaders().getContentType()
                : MediaType.APPLICATION_JSON;
        headers.setContentType(contentType);

        String responseBody = ex.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isBlank()) {
            return ResponseEntity.status(ex.getStatusCode()).headers(headers).body(responseBody);
        }

        Map<String, Object> fallbackBody = new HashMap<>();
        fallbackBody.put("timestamp", LocalDateTime.now());
        fallbackBody.put("status", ex.getStatusCode().value());
        fallbackBody.put("error", ex.getStatusText());
        fallbackBody.put("message", "Error devuelto por el servicio downstream: " + ex.getMessage());
        return ResponseEntity.status(ex.getStatusCode()).headers(headers).body(fallbackBody);
    }

    /**
     * Captura fallos de conectividad hacia microservicios downstream -> 503 Service Unavailable.
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(ResourceAccessException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("message", "No fue posible comunicarse con el microservicio de destino. Verifique que se encuentre en ejecución. Detalle: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * Captura validaciones de campos (@Valid en los DTOs de entrada) -> 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Errores de validación en la solicitud enviada al BFF");
        body.put("fieldErrors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura cualquier otra excepción genérica no controlada -> 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "Ocurrió un error inesperado en el BFF: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
