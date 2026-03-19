# Challenge Técnico - Desarrollador Backend Java

Implementación de **dos microservicios** que se comunican por **HTTP** usando **Spring Boot 3.x + Java 17**, con enfoque en **programación funcional/reactiva**, **WebFlux**, **WebClient**, persistencia en **H2** (reactivo con **R2DBC**) y **tests unitarios + integración** levantando ambos servicios.

---

## Objetivo del challenge (resumen)

Desarrollar dos microservicios que se comuniquen entre sí, aplicando:

- **Spring Boot 3.x + Java 17** (Records, patrones funcionales)
- **Comunicación HTTP** (WebClient)
- **Programación funcional** (Streams, Lambdas, Optional, Inmutabilidad)
- **Programación reactiva** (WebFlux: endpoints no bloqueantes)
- **Pruebas unitarias** (JUnit 5 + Mockito) + assertions (AssertJ)
- **H2 o similar** como repositorio de base de datos

---

## Arquitectura y módulos

Proyecto **multi-módulo Maven**:

- `orders-service` (**Servicio de Pedidos**): expone `POST /orders`, orquesta la creación del pedido y llama a Inventario con **WebClient**.
- `inventory-service` (**Servicio de Inventario**): expone `POST /inventory/allocate`, valida existencia/stock y descuenta stock en **H2**.
- `common`: contratos compartidos (`record` DTOs) y `ApiError`.

Paquetes por capa (por servicio):
`controller`, `service`, `client`, `repository`, `domain`, `dto`, `error`, `config`.

---

## Requisitos para ejecutar

- **Java 17**
- **Maven** (3.9+ recomendado)

---

## Cómo ejecutar los servicios (local)

Ejecuta desde la raíz del proyecto (donde está el `pom.xml` padre).

### Paso 0) Instalar módulos (sin ejecutar tests)

```bash
mvn -DskipTests install
```

Luego, en dos terminales:

### 1) Inventory (puerto 8082)

```bash
mvn -pl inventory-service spring-boot:run
```

### 2) Orders (puerto 8081)

```bash
mvn -pl orders-service spring-boot:run
```

Por defecto `orders-service` apunta a Inventario en `http://localhost:8082` (ver `orders-service/src/main/resources/application.yml`).

---

## Endpoints principales

### Inventory

- **POST** `/inventory/allocate`

Request:

```json
{
  "orderId": "order-1",
  "items": [
    { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 1 }
  ]
}
```

Responses:
- `200 OK`: asigna stock y devuelve ítems asignados.
- `400 BAD_REQUEST`: request inválido.
- `404 NOT_FOUND`: producto no existe.
- `409 CONFLICT`: stock insuficiente.

### Orders

- **POST** `/orders`

Request:

```json
{
  "customerId": "cust-1",
  "items": [
    { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 2 },
    { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 3 },
    { "productId": "22222222-2222-2222-2222-222222222222", "quantity": 1 }
  ]
}
```

Responses:
- `201 CREATED`: pedido confirmado (`CONFIRMED`) y lista de ítems asignados.
- `400 BAD_REQUEST`: request inválido.
- `404 NOT_FOUND`: producto inexistente (propagado/traducido desde Inventario).
- `409 CONFLICT`: stock insuficiente (propagado/traducido desde Inventario).
- `503 SERVICE_UNAVAILABLE`: Inventario no disponible o timeout.

### Obtener pedido

- **GET** `/orders/{orderId}`

Responses:
- `200 OK`: devuelve el pedido con sus ítems.
- `404 NOT_FOUND`: pedido no encontrado (`ApiError.code = "ORDER_NOT_FOUND"`).

---

## Formato de error (ApiError)

Las respuestas de error siguen el contrato:

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Producto no encontrado",
  "details": ["<productId>"]
}
```

Implementado en `common/src/main/java/com/challenge/common/api/ApiError.java` y traducido vía `@RestControllerAdvice` en cada servicio.

---

## Cómo correr los tests

Ejecuta toda la suite (unitarios + integración) desde la raíz:

```bash
mvn test -am
```

Incluye:
- **Unit tests** de Service (Mockito + JUnit 5 + AssertJ + StepVerifier)
- **Unit tests** de Controller (`@WebFluxTest` + `WebTestClient`)
- **Integración real** levantando ambos microservicios en **puertos random**:
  - `orders-service/src/test/java/com/challenge/orders/OrdersInventoryIntegrationTest.java`

Tests del endpoint `GET /orders/{orderId}`:
- `orders-service/src/test/java/com/challenge/orders/controller/OrdersControllerTest.java`
- `orders-service/src/test/java/com/challenge/orders/service/OrdersServiceTest.java`

### Datos (seed) para integración

Para el perfil `test`, los esquemas/datos se inicializan con scripts SQL por módulo:

- Inventario: `inventory-service/src/test/resources/inventory/schema.sql` y `inventory-service/src/test/resources/inventory/data.sql`
- Pedidos: `orders-service/src/test/resources/orders/schema.sql` y `orders-service/src/test/resources/orders/data.sql`

Y se asegura el seed en la misma H2 en memoria usada por R2DBC mediante:
- `inventory-service/src/main/java/com/challenge/inventory/config/InventoryTestDatabaseSeeder.java`
- `orders-service/src/main/java/com/challenge/orders/config/OrdersTestDatabaseSeeder.java`

---

## Evidencia de cumplimiento (criterios de evaluación)

### Programación Funcional / Reactiva (Obligatorio)

- **Optional** (evitar NPE / construir decisiones y excepciones):
  - `inventory-service/src/main/java/com/challenge/inventory/service/DefaultInventoryService.java`
  - `orders-service/src/main/java/com/challenge/orders/service/DefaultOrdersService.java`
  - `orders-service/src/main/java/com/challenge/orders/client/WebClientInventoryClient.java` (parse defensivo de `details`)

- **Supplier / Consumer / Predicate**:
  - `DefaultInventoryService` y `DefaultOrdersService` (validaciones, construcción lazy de excepciones, auditoría/logs)

- **Streams (filter/map/toList/collect)**:
  - `DefaultInventoryService` y `DefaultOrdersService` (normalización de ítems, `Collectors.toMap`, `toList`)

- **Lambdas (sin clases anónimas)**:
  - Predicados, mappers y callbacks están expresados como lambdas en services y cliente WebClient.

- **WebFlux endpoints no bloqueantes**:
  - `inventory-service/src/main/java/com/challenge/inventory/controller/InventoryController.java`
  - `orders-service/src/main/java/com/challenge/orders/controller/OrdersController.java`

- **Manejo de errores reactivo**:
  - `orders-service/src/main/java/com/challenge/orders/client/WebClientInventoryClient.java` (`onStatus`, `timeout`, `onErrorResume`)
  - `inventory-service/src/main/java/com/challenge/inventory/error/InventoryExceptionHandler.java`
  - `orders-service/src/main/java/com/challenge/orders/error/OrdersExceptionHandler.java`

- **WebClient (en lugar de RestTemplate)**:
  - `orders-service/src/main/java/com/challenge/orders/client/WebClientInventoryClient.java`

### Pruebas Unitarias

- **Service** (Mockito + JUnit 5 + AssertJ + StepVerifier):
  - `inventory-service/src/test/java/com/challenge/inventory/service/InventoryServiceTest.java`
  - `orders-service/src/test/java/com/challenge/orders/service/OrdersServiceTest.java`

- **Controller** (`@WebFluxTest` + `WebTestClient` + mocks):
  - `inventory-service/src/test/java/com/challenge/inventory/controller/InventoryControllerTest.java`
  - `orders-service/src/test/java/com/challenge/orders/controller/OrdersControllerTest.java`

- **Integración entre microservicios** (levantando ambos servicios):
  - `orders-service/src/test/java/com/challenge/orders/OrdersInventoryIntegrationTest.java`

---

## Requisitos adicionales

- **Documentación**: Javadoc en clases públicas principales (API/servicios/clientes/handlers) y tests.
- **Buenas prácticas**: separación por capas, DTOs inmutables (`record`), manejo centralizado de errores.
- **Excepciones**: custom exceptions por caso de negocio y traducción a `ApiError` consistente.
- **Estructura organizada**: multi-módulo + `common` para contratos compartidos.
- **Dependencias**: Spring Boot 3.x, Java 17, WebFlux, WebClient, R2DBC + H2, JUnit 5, Mockito, AssertJ, reactor-test.

