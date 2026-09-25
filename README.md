# 📦 ms-pedidos360-bff

**Backend For Frontend (BFF) - Seguridad OAuth2/JWT y Orquestación de Microservicios**  
*Sistema de Gestión de Pedidos - Pedidos360*

---

## 🎯 Propósito del Microservicio (40% de la Evaluación)

El **BFF (Backend For Frontend)** actúa como la puerta de enlace de seguridad y el punto de entrada unificado para las aplicaciones cliente (web/móvil).  
**No posee capa de persistencia propia a base de datos**; su responsabilidad fundamental radica en:
1. **Seguridad Perimetral Centralizada**: Valida la firma criptográfica, la vigencia temporal y el **Audience** (`aud`) de los JSON Web Tokens (JWT) emitidos por **Microsoft Entra ID (Azure AD)**.
2. **Transformación de Roles (RBAC)**: Mapea el claim `roles` proveniente de Entra ID al formato estándar de Spring Security (`ROLE_<ROL>`).
3. **Control de Acceso Basado en Roles (RBAC)**: Aplica una matriz estricta de permisos según el perfil del usuario (**CLIENTE**, **OPERADOR**, **ADMIN**).
4. **Propagación Segura de Identidad**: Extrae la identidad del cliente (`oid` o `sub`) desde el JWT y la inyecta de forma transparente en los encabezados y parámetros de las peticiones dirigidas a los microservicios internos.
5. **Orquestación y Enrutamiento HTTP**: Reenvía las peticiones a los microservicios downstream mediante `RestClient`:
   - `ms-pedidos360-catalogo` (Puerto `8081`)
   - `ms-pedidos360-orders` (Puerto `8082`)
6. **Manejo Global de Excepciones**: Centraliza respuestas homogéneas en formato JSON para accesos no autorizados (`401`), accesos denegados (`403`), servicios no disponibles (`503`) y errores devueltos por servicios downstream.

---

## 🏗️ Arquitectura y Estructura del Proyecto

```
com.pedidos360.bff
├── BffApplication.java                      # Clase principal y definición de beans
├── config/
│   ├── SecurityConfig.java                 # Configuración de Spring Security, filtros y validadores
│   ├── RoleConverter.java                  # JwtAuthenticationConverter para claims 'roles' de Entra ID
│   └── AudienceValidator.java              # Validador de token para claim 'aud'
├── controller/
│   ├── OrderProxyController.java           # Proxy seguro hacia el microservicio de órdenes
│   └── CatalogProxyController.java         # Proxy seguro hacia el catálogo de productos
├── client/
│   ├── OrdersClient.java                   # Cliente HTTP (RestClient) hacia ms-pedidos
│   └── CatalogClient.java                  # Cliente HTTP (RestClient) hacia ms-catalogo
├── dto/
│   ├── OrderRequestDTO.java                # DTO de solicitud de pedido
│   ├── OrderItemRequestDTO.java            # DTO de ítem dentro de un pedido
│   ├── StatusUpdateDTO.java                # DTO de cambio de estado
│   └── ProductRequestDTO.java              # DTO para alta/edición de producto
└── exception/
    └── GlobalExceptionHandler.java         # Manejador global de excepciones (401, 403, 503, validaciones)
```

---

## 🛡️ Matriz de Control de Acceso (RBAC)

Diferenciación mínima entre actores configurada en [SecurityConfig.java](file:///c:/Users/B/Desktop/ms-pedidos360-bff/src/main/java/com/pedidos360/bff/config/SecurityConfig.java):

| Recurso / Endpoint | Método HTTP | Roles Permitidos | Descripción |
| :--- | :---: | :---: | :--- |
| `/api/catalog/products` | `GET` | **CLIENTE, OPERADOR, ADMIN** | Listar productos disponibles del catálogo |
| `/api/catalog/products/{id}` | `GET` | **CLIENTE, OPERADOR, ADMIN** | Consultar detalle de un producto específico |
| `/api/catalog/products` | `POST` | **ADMIN** | Crear nuevos productos en el catálogo |
| `/api/catalog/products/{id}` | `PUT` | **ADMIN, OPERADOR** | Modificar datos de productos existentes |
| `/api/catalog/products/{id}` | `DELETE` | **ADMIN** | Eliminación lógica (*soft delete*) de productos |
| `/api/orders/mine` | `GET` | **CLIENTE** (Cualquiera autenticado) | Ver únicamente los pedidos propios del usuario |
| `/api/orders` | `POST` | **CLIENTE, ADMIN** | Crear un nuevo pedido a nombre del cliente del token |
| `/api/orders` | `GET` | **OPERADOR, ADMIN** | Listar la totalidad de pedidos del sistema |
| `/api/orders/{id}` | `GET` | **OPERADOR, ADMIN** | Ver el detalle de cualquier pedido por ID |
| `/api/orders/{id}/status` | `PATCH` | **OPERADOR, ADMIN** | Avanzar o cambiar el estado según la máquina de estados |

---

## 🔑 Integración con Microsoft Entra ID (Azure AD)

### 1. Validación de Audiencia Personalizada (`AudienceValidator`)
Spring Security con `issuer-uri` valida automáticamente la firma y la vigencia temporal del token consultando los certificados de Azure (`.well-known/openid-configuration` y JWKS). Sin embargo, **no valida por defecto el claim `aud`**, lo que permitiría que un token emitido para otra API de Azure fuera aceptado.  
En este BFF se implementó `AudienceValidator` combinándolo con los validadores predeterminados:

```java
@Bean
public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
    OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefaultWithIssuer(issuerUri);
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, audienceValidator));
    return decoder;
}
```

### 2. Mapeo de Roles de Entra ID (`RoleConverter`)
Entra ID entrega los App Roles en un claim tipo array de strings llamado `roles`:
```json
{
  "aud": "api://<API_CLIENT_ID>",
  "iss": "https://login.microsoftonline.com/<TENANT_ID>/v2.0",
  "sub": "usuario-sub-uuid",
  "oid": "usuario-oid-uuid",
  "roles": ["OPERADOR"]
}
```
El `RoleConverter` convierte cada entrada en una autoridad `GrantedAuthority` con el prefijo estándar `ROLE_`:
- `"ADMIN"` ➔ `ROLE_ADMIN`
- `"OPERADOR"` ➔ `ROLE_OPERADOR`
- `"CLIENTE"` ➔ `ROLE_CLIENTE`

### 3. Extracción de Identidad del Cliente
En `OrderProxyController`, la identidad del cliente se resuelve automáticamente del JWT:
```java
String clienteId = jwt.getClaimAsString("oid"); // Si existe, prioriza el Object ID
if (clienteId == null || clienteId.isBlank()) {
    clienteId = jwt.getSubject(); // Fallback a 'sub'
}
```
Y se reenvía a `ms-pedidos360-orders` mediante:
- Encabezado HTTP: `X-Cliente-Id: <clienteId>`
- Query parameter: `?clienteId=<clienteId>`

---

## ⚙️ Configuración y Variables de Entorno

El archivo `application.yml` utiliza variables de entorno con valores por defecto. Para configurarlo localmente, crea un archivo `.env` tomando como base `.env.example`:

```properties
# Puerto del BFF
SERVER_PORT=8080

# Azure Entra ID
AZURE_TENANT_ID=<TENANT_ID>
AZURE_CLIENT_ID=<API_CLIENT_ID>
JWT_ISSUER_URI=https://login.microsoftonline.com/<TENANT_ID>/v2.0
JWT_AUDIENCE=api://<API_CLIENT_ID>

# Microservicios Downstream
ORDERS_SERVICE_URL=http://localhost:8082
CATALOG_SERVICE_URL=http://localhost:8081
```

---

## 🚀 Compilación y Ejecución

### Prerrequisitos
- **Java 17** o superior
- **Maven 3.8+** (o el bundled Maven de IntelliJ / Eclipse / VS Code)

### Comandos de Compilación y Pruebas
```powershell
# Compilar y ejecutar pruebas unitarias y de integración de seguridad
mvn clean test

# Generar el archivo JAR ejecutable
mvn clean package
```

### Ejecutar la Aplicación
```powershell
# Usando Spring Boot plugin
mvn spring-boot:run

# O ejecutando el JAR generado
java -jar target/bff-0.0.1-SNAPSHOT.jar
```

---

## 🧪 Pruebas Automatizadas Incluidas

El proyecto incluye una suite de pruebas que valida:
1. [RoleConverterTest.java](file:///c:/Users/B/Desktop/ms-pedidos360-bff/src/test/java/com/pedidos360/bff/config/RoleConverterTest.java):
   - Mapeo de `roles` a `ROLE_<ROL>`.
   - Idempotencia si el prefijo ya está presente.
   - Manejo seguro de JWT sin claim `roles` o nulo.
2. [AudienceValidatorTest.java](file:///c:/Users/B/Desktop/ms-pedidos360-bff/src/test/java/com/pedidos360/bff/config/AudienceValidatorTest.java):
   - Aprobación cuando el claim `aud` coincide.
   - Rechazo inmediato (`invalid_token`) cuando el audience es erróneo.
   - Compatibilidad en entorno de desarrollo.
3. [SecurityAndProxyControllerTest.java](file:///c:/Users/B/Desktop/ms-pedidos360-bff/src/test/java/com/pedidos360/bff/controller/SecurityAndProxyControllerTest.java):
   - **401 Unauthorized**: Peticiones anónimas sin Bearer Token.
   - **403 Forbidden**: Peticiones de CLIENTE a endpoints de administración o cambio de estado.
   - **403 Forbidden**: Peticiones de OPERADOR intentando crear o borrar productos.
   - **200/201 OK**: Peticiones autorizadas para cada rol.
   - **Aislamiento de Órdenes**: CLIENTE solo accede a `/api/orders/mine` y no a `/api/orders`.

---

## 📡 Ejemplos de Peticiones con cURL

### 1. Consultar Catálogo de Productos (Cualquier rol autenticado)
```bash
curl -X GET "http://localhost:8080/api/catalog/products" \
  -H "Authorization: Bearer <TOKEN_JWT>" \
  -H "Accept: application/json"
```

### 2. Crear Producto (Solo rol ADMIN)
```bash
curl -X POST "http://localhost:8080/api/catalog/products" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Teclado Mecánico RGB",
    "descripcion": "Switches Red, formato TKL",
    "precio": 59990.00,
    "stock": 25
  }'
```

### 3. Crear Pedido (Rol CLIENTE)
```bash
curl -X POST "http://localhost:8080/api/orders" \
  -H "Authorization: Bearer <TOKEN_CLIENTE>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productoId": 1,
        "cantidad": 2,
        "precioUnitario": 59990.00
      }
    ]
  }'
```

### 4. Consultar Mis Pedidos (Rol CLIENTE)
```bash
curl -X GET "http://localhost:8080/api/orders/mine" \
  -H "Authorization: Bearer <TOKEN_CLIENTE>"
```

### 5. Cambiar Estado de un Pedido (Rol OPERADOR o ADMIN)
```bash
curl -X PATCH "http://localhost:8080/api/orders/1/status" \
  -H "Authorization: Bearer <TOKEN_OPERADOR>" \
  -H "Content-Type: application/json" \
  -d '{
    "estado": "ACEPTADO"
  }'
```
