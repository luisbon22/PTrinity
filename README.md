# PTrinity · Sistema de gestión para una entidad financiera

Aplicación para los trabajadores de una entidad financiera que permite administrar **clientes**, sus **productos financieros** (cuentas de ahorros y corrientes) y los **movimientos** sobre ellos (consignaciones, retiros y transferencias), además de consultar **estados de cuenta**.

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Base de datos | PostgreSQL 16 · migraciones con Flyway |
| Frontend | SPA en HTML, CSS y JavaScript servida por Spring Boot |
| Pruebas | JUnit 5 · Mockito · MockMvc · AssertJ · JaCoCo |
| Infraestructura | Docker (multi-stage) · Docker Compose · GitHub Actions |
| Documentación API | OpenAPI 3 / Swagger UI |

---

## 1. Cómo ejecutarlo

### Opción A: Docker (recomendada)

```bash
docker compose up -d --build
```

- Interfaz web: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Salud: http://localhost:8080/actuator/health

Si el puerto 8080 está ocupado: `APP_PORT=8081 docker compose up -d --build`.

La base de datos arranca con datos de demostración (3 clientes, 3 cuentas y sus movimientos iniciales).

### Opción B: sin Docker (H2 en memoria emulando PostgreSQL)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Pruebas y cobertura

```bash
./mvnw verify
```

El reporte de cobertura queda en `target/site/jacoco/index.html`.

---

## 2. Requerimientos funcionales y cómo se cumplen

### Clientes
| Requerimiento | Implementación |
|---|---|
| Crear con id, tipo y número de identificación, nombres, apellido, correo, fecha de nacimiento, fecha de creación y de modificación | `Customer` + tabla `customers` |
| Modificar; la fecha de modificación se calcula automáticamente | `Customer.update()` asigna `updatedAt` con el `Clock` del sistema |
| Eliminar | `DELETE /api/customers/{id}` |
| No puede existir si es menor de edad | Validación en el dominio **y** `CHECK ck_customers_adult` en la base de datos |
| No se elimina si tiene productos vinculados | Validación en `CustomerService` **y** `FOREIGN KEY ... ON DELETE RESTRICT` |
| Fecha de creación automática | `Customer.create()` |
| *(Opcional)* Formato de correo `xxxx@xxxxx.xxx` | Bean Validation en el DTO, regla en el dominio y validación en el front |
| *(Opcional)* Nombre y apellido de mínimo 2 caracteres | DTO, dominio y `CHECK` en base de datos |
| *(Adicional)* Identificación y correo únicos | `UNIQUE` en base de datos y validación en el servicio (HTTP 409) |

### Productos (cuentas)
| Requerimiento | Implementación |
|---|---|
| Solo cuenta corriente o de ahorros | Enum `AccountType` + `CHECK ck_accounts_type` |
| Solo existe vinculada a un cliente | `customer_id NOT NULL` + FK; el servicio valida que el cliente exista |
| Atributos: id, tipo, número, estado, saldo, exenta GMF, fechas, cliente | `Account` + tabla `accounts` |
| Ahorros no puede tener saldo menor a $0 | `Account.debit()` + `CHECK ck_accounts_savings_non_negative` |
| Activar o inactivar en cualquier momento | `PATCH /api/accounts/{id}/status` |
| Número único de 10 dígitos, generado automáticamente; ahorros inicia en `53` y corriente en `33` | Secuencias de PostgreSQL + `AccountType.formatAccountNumber()`; `UNIQUE` y `CHECK` de formato |
| La cuenta de ahorros se crea activa | `Account.open()` crea toda cuenta en estado `ACTIVE` |
| Solo se cancelan cuentas con saldo $0 | `Account.cancel()` + `CHECK ck_accounts_cancelled_zero_balance` |
| Fecha de creación automática; el saldo se actualiza con cada transacción exitosa | `Account.open()`, `credit()` y `debit()` |

### Transacciones
| Requerimiento | Implementación |
|---|---|
| Solo consignación, retiro y transferencia | `POST /api/transactions/{deposits,withdrawals,transfers}` |
| Actualiza el saldo y el saldo disponible | `balance` y `availableBalance = balance + cupo de sobregiro` |
| Transferencias solo entre cuentas existentes; genera débito en origen y crédito en destino | `TransactionService.transfer()` registra dos movimientos con la misma `reference` |

**Decisiones de negocio adicionales** (configurables y documentadas):
- **Cupo de sobregiro**: las cuentas corrientes tienen un cupo de sobregiro (`bank.checking.overdraft-limit`, por defecto $1.000.000) que explica la diferencia entre *saldo* y *saldo disponible*. Las de ahorros no tienen sobregiro.
- **GMF (4 × 1.000)**: si la cuenta **no** está exenta, cada débito (retiro o transferencia saliente) cobra el 0,4 % adicional, que queda registrado en el movimiento.
- Las cuentas inactivas o canceladas no admiten movimientos, y una cuenta cancelada no puede reactivarse.
- Los movimientos son **registros contables inmutables**: se crean y se consultan, pero no se editan ni se eliminan. Por eso el CRUD de transacciones expone *Create* y *Read*.

---

## 3. Arquitectura: hexagonal (puertos y adaptadores)

```
                   ┌──────────────────────────────────────────────┐
  HTTP / Front ──► │  Adaptador de entrada (REST controllers)     │
                   └────────────────────┬─────────────────────────┘
                                        │ usa puertos de entrada
                   ┌────────────────────▼─────────────────────────┐
                   │  Aplicación (services = casos de uso)        │
                   │  ┌────────────────────────────────────────┐  │
                   │  │ Dominio: Customer, Account, Transaction│  │
                   │  │ reglas de negocio, puertos (interfaces)│  │
                   │  └────────────────────────────────────────┘  │
                   └────────────────────┬─────────────────────────┘
                                        │ usa puertos de salida
                   ┌────────────────────▼─────────────────────────┐
                   │  Adaptadores de salida (JPA, secuencias)     │ ──► PostgreSQL
                   └──────────────────────────────────────────────┘
```

```
com.ptrinity.bank
├── domain                         ← núcleo sin dependencias de frameworks
│   ├── model                      ← entidades de dominio y value objects
│   ├── exception
│   └── port
│       ├── in                     ← casos de uso (CustomerUseCase, AccountUseCase, TransactionUseCase)
│       └── out                    ← repositorios y generador de números de cuenta
├── application/service            ← capa SERVICE: implementa los casos de uso
└── infrastructure
    ├── adapter/in/web
    │   ├── controller             ← capa CONTROLLER (REST)
    │   ├── dto · mapper · exception
    ├── adapter/out/persistence
    │   ├── entity                 ← capa ENTITY (JPA)
    │   ├── repository             ← capa REPOSITORY (Spring Data JPA)
    │   └── mapper · *Adapter
    └── config
```

Así se cumple también la separación por capas pedida (**entity, service, controller, repository**), con la diferencia de que el dominio no conoce a JPA ni a HTTP: los controladores dependen de interfaces (`*UseCase`) y los servicios dependen de puertos (`*RepositoryPort`), no de implementaciones.

### Componentes
| Componente | Responsabilidad |
|---|---|
| `CustomerController`, `AccountController`, `TransactionController` | Exponen la API REST y validan la entrada |
| `GlobalExceptionHandler` | Traduce las excepciones a respuestas HTTP homogéneas (400, 404, 409, 422) |
| `CustomerService`, `AccountService`, `TransactionService` | Orquestan los casos de uso dentro de transacciones |
| `Customer`, `Account`, `Transaction`, `AccountStatement` | Modelo de dominio con las reglas de negocio |
| `*PersistenceAdapter` | Implementan los puertos de salida con Spring Data JPA |
| `SequenceAccountNumberGenerator` | Genera números de cuenta únicos con secuencias de base de datos |
| Frontend (`static/`) | Interfaz web que consume la API |

---

## 4. Patrones de diseño utilizados

| Patrón | Dónde |
|---|---|
| **Puertos y adaptadores (Hexagonal)** | Toda la estructura del proyecto |
| **Repository** | `CustomerRepositoryPort`, `AccountRepositoryPort` y sus adaptadores JPA |
| **Adapter** | `*PersistenceAdapter` adapta Spring Data al puerto del dominio |
| **Factory Method** | `Customer.create()`, `Account.open()`, `Transaction.credit()/debit()`, `AccountStatement.of()` |
| **DTO** | `dto/*Request`, `dto/*Response`, que desacoplan el contrato HTTP del dominio |
| **Mapper** | `WebMapper` y `*PersistenceMapper` |
| **Command** | `OpenAccountCommand`, `DepositCommand`, `WithdrawalCommand`, `TransferCommand` |
| **Dependency Injection** | Inyección por constructor en todos los componentes |
| **Strategy (vía enum)** | `AccountType` encapsula el prefijo y el formato del número de cuenta por tipo |
| **Value Object** | `CustomerData` y los records inmutables |
| **Controller Advice** | `GlobalExceptionHandler` centraliza el manejo de errores |
| **Rich Domain Model** | Las reglas viven en las entidades (`Account.debit()`, `Account.cancel()`), no en los servicios |

---

## 5. Principios SOLID

- **S, responsabilidad única**: cada clase tiene un motivo de cambio. Los controladores solo traducen HTTP, los servicios orquestan, las entidades de dominio protegen sus reglas, los mappers convierten y los adaptadores persisten.
- **O, abierto/cerrado**: agregar un nuevo tipo de cuenta solo requiere añadir un valor al enum `AccountType` (con su prefijo) y su secuencia. Cambiar la persistencia (por ejemplo, a otro motor) se hace con un nuevo adaptador sin tocar el dominio.
- **L, sustitución de Liskov**: cualquier implementación de un puerto (por ejemplo, los *mocks* de las pruebas) puede reemplazar al adaptador real sin alterar el comportamiento de los servicios.
- **I, segregación de interfaces**: un caso de uso por agregado (`CustomerUseCase`, `AccountUseCase`, `TransactionUseCase`) y puertos de salida pequeños y específicos (`AccountNumberGeneratorPort` está separado del repositorio).
- **D, inversión de dependencias**: el dominio define las interfaces y la infraestructura las implementa. Los servicios dependen de abstracciones (`*Port`) y los controladores de `*UseCase`. Incluso el reloj (`java.time.Clock`) se inyecta.

---

## 6. Propiedades ACID

| Propiedad | Cómo se garantiza |
|---|---|
| **Atomicidad** | Cada caso de uso es `@Transactional`. En una transferencia, el débito, el crédito y los dos movimientos se confirman juntos o se revierten juntos ante cualquier excepción |
| **Consistencia** | Las reglas se validan en el dominio y además se refuerzan en la base de datos con `CHECK` (ahorros ≥ 0, saldo ≥ −sobregiro, cancelada ⇒ saldo 0, mayoría de edad, formato del número de cuenta), `UNIQUE` y `FOREIGN KEY` |
| **Aislamiento** | Las cuentas se leen con **bloqueo pesimista** (`SELECT ... FOR UPDATE`) antes de modificarlas, y en las transferencias se bloquean siempre en el mismo orden para evitar interbloqueos. Las cuentas además tienen `@Version` (bloqueo optimista) como segunda barrera |
| **Durabilidad** | PostgreSQL persiste las transacciones confirmadas (WAL) en un volumen Docker (`ptrinity-data`) |

---

## 7. Base de datos: DDL y DML

Los scripts están versionados con **Flyway** y se ejecutan automáticamente al arrancar:

- **DDL**: [`src/main/resources/db/migration/V1__create_schema.sql`](src/main/resources/db/migration/V1__create_schema.sql). Tablas `customers`, `accounts` y `transactions`, índices, restricciones y las secuencias `savings_account_number_seq` y `checking_account_number_seq`.
- **DML**: [`src/main/resources/db/seed/V2__seed_demo_data.sql`](src/main/resources/db/seed/V2__seed_demo_data.sql). Datos de demostración (`INSERT` y ajuste de secuencias). No se carga en las pruebas automatizadas.

Hibernate trabaja con `ddl-auto=validate`, de modo que el esquema lo define únicamente el DDL versionado.

```
customers 1 ──── * accounts 1 ──── * transactions
```

---

## 8. API REST

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/customers` | Lista los clientes |
| GET | `/api/customers/{id}` | Consulta un cliente |
| POST | `/api/customers` | Crea un cliente |
| PUT | `/api/customers/{id}` | Actualiza un cliente |
| DELETE | `/api/customers/{id}` | Elimina un cliente sin productos |
| GET | `/api/accounts?customerId=` | Lista las cuentas (filtro opcional) |
| GET | `/api/accounts/{id}` | Consulta una cuenta |
| POST | `/api/accounts` | Abre una cuenta `{customerId, accountType, gmfExempt}` |
| PATCH | `/api/accounts/{id}/status` | Cambia el estado `{status: ACTIVE \| INACTIVE \| CANCELLED}` |
| GET | `/api/accounts/{id}/statement` | Estado de cuenta |
| GET | `/api/transactions?accountId=` | Lista los movimientos |
| POST | `/api/transactions/deposits` | Consignación `{accountNumber, amount, description}` |
| POST | `/api/transactions/withdrawals` | Retiro `{accountNumber, amount, description}` |
| POST | `/api/transactions/transfers` | Transferencia `{sourceAccountNumber, destinationAccountNumber, amount, description}` |

Códigos de respuesta: `201` creado · `204` eliminado · `400` datos inválidos · `404` no encontrado · `409` duplicado o conflicto · `422` regla de negocio incumplida.

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/transactions/transfers \
  -H "Content-Type: application/json" \
  -d '{"sourceAccountNumber":"5300000001","destinationAccountNumber":"3300000001","amount":150000}'
```

---

## 9. Pruebas unitarias

79 pruebas automatizadas con JUnit 5:

| Suite | Tipo | Qué cubre |
|---|---|---|
| `CustomerTest`, `AccountTest` | Unitarias de dominio | Reglas de negocio puras |
| `CustomerServiceTest`, `AccountServiceTest`, `TransactionServiceTest` | Unitarias (Mockito) | **Capa Service** con puertos simulados |
| `CustomerControllerTest`, `AccountControllerTest`, `TransactionControllerTest` | `@WebMvcTest` + MockMvc | **Capa Controller**: rutas, validaciones y códigos HTTP |
| `PersistenceAdapterTest` | `@DataJpaTest` | Adaptadores JPA y restricciones del esquema real |
| `BankingFlowIntegrationTest` | `@SpringBootTest` | Flujo completo de punta a punta |

Cobertura de instrucciones (JaCoCo): **Service ≈ 93 %**, **Controller 100 %**.

---

## 10. Servicios en la nube

- **GitHub**: repositorio remoto del código fuente.
- **GitHub Actions** ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)): en cada *push* o *pull request* compila, ejecuta las pruebas, publica el reporte de cobertura y construye la imagen Docker en los *runners* de GitHub.
- **Despliegue propuesto** (no está desplegado; la imagen está lista para ello): imagen en Amazon ECR, ejecución en **AWS ECS Fargate** (o Azure Container Apps), base de datos administrada **Amazon RDS for PostgreSQL** y credenciales en **AWS Secrets Manager**, inyectadas mediante las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` que la aplicación ya soporta.

---

## 11. Control de versiones: Git Flow

```
main      ●─────────────────────────────────────────────●  v1.0.0
           \                                            /
develop     ●───●───────●───────●───────●───────●──────●
                 \     / \     / \     / \     / \     /
feature/*         ●───●   ●───●   ●───●   ●───●   ●───●
```

- `main`: versiones liberadas (etiquetadas, por ejemplo `v1.0.0`).
- `develop`: rama de integración.
- `feature/*`: una rama por funcionalidad (`domain-model`, `persistence`, `application-services`, `rest-api`, `frontend`, `docker`, `docs`), integradas a `develop` con `--no-ff` para conservar el historial.
- `release/*`: preparación de una versión antes de pasar a `main`.
- Mensajes de commit con la convención *Conventional Commits* (`feat`, `fix`, `test`, `build`, `docs`, `chore`).

---

## 12. Frontend

Interfaz web responsive servida en `/`:

- **Resumen**: indicadores (clientes, cuentas activas, saldo total, movimientos), últimos movimientos y saldo por tipo de cuenta.
- **Clientes**: búsqueda, registro, edición y eliminación (se bloquea si el cliente tiene productos).
- **Cuentas**: apertura, activación, inactivación, cancelación y estado de cuenta imprimible.
- **Movimientos**: consignaciones, retiros y transferencias, con saldo disponible y aviso de GMF, más el historial filtrable.
