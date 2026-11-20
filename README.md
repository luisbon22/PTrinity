# PTrinity

Aplicación para los trabajadores de una entidad financiera: administra **clientes**, sus **productos financieros** (cuentas de ahorros y corrientes) y las **transacciones** sobre ellos (consignaciones, retiros y transferencias), y genera **estados de cuenta**.

| Componente | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Base de datos | PostgreSQL 16 · migraciones versionadas con Flyway |
| Frontend | React 19 · TypeScript · Vite · servido por Nginx |
| Documentación API | OpenAPI 3 · Swagger UI (springdoc) |
| Pruebas | JUnit 5 · Mockito · MockMvc · AssertJ · JaCoCo / Vitest · Testing Library |
| Infraestructura | Docker multi-etapa · Docker Compose · GitHub Actions · GitHub Container Registry |

---

## 1. Cómo ejecutarlo

### Con Docker (recomendado)

```bash
docker compose --env-file env/dev.env up -d --build
```

| Servicio | URL |
|---|---|
| Interfaz web | http://localhost:3000 |
| Swagger UI | http://localhost:3000/swagger-ui.html (o http://localhost:8080/swagger-ui.html) |
| API REST | http://localhost:8080/api |
| Salud | http://localhost:8080/actuator/health |

La imagen del backend corre las pruebas durante el build: si la cobertura baja del 85 %, la imagen no se construye. Para un build rápido: `OMITIR_PRUEBAS=true docker compose --env-file env/dev.env up -d --build`.

### Sin Docker

```bash
# Backend con H2 en memoria (modo PostgreSQL) y datos de demostración
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Frontend (en otra terminal); /api se redirige a localhost:8080
cd frontend && npm install && npm run dev
```

### Pruebas y cobertura

```bash
cd backend && ./mvnw verify            # 101 pruebas; falla si la cobertura < 85 %
cd frontend && npm run test:coverage   # 33 pruebas; falla si la cobertura < 85 %
```

| Proyecto | Líneas | Ramas | Reporte |
|---|---|---|---|
| Backend | 100 % | 98,8 % | `backend/target/site/jacoco/index.html` |
| Frontend | 100 % | 97,2 % | `frontend/coverage/index.html` |

El umbral está en `backend/pom.xml` (JaCoCo `check`, global y específico para los paquetes `service` y `controller`) y en `frontend/vite.config.ts`.

---

## 2. Ambientes

Hay tres ambientes, cada uno ligado a una rama y a un perfil de Spring:

| Ambiente | Rama | Perfil | Datos demo | Swagger | Compose | Puertos web / API / BD |
|---|---|---|---|---|---|---|
| Desarrollo | `dev` | `dev` | Sí | Sí | `env/dev.env` | 3000 / 8080 / 5432 |
| QA | `qa` | `qa` | Sí | Sí | `env/qa.env` | 3100 / 8180 / 5433 |
| Producción | `main` | `prod` | No | No | `env/prod.env` (copiar de `prod.env.example`) | 80 / 8280 / 5434 |

Cada ambiente usa su propio proyecto de Compose (`ptrinity-dev`, `ptrinity-qa`, `ptrinity-prod`), base de datos y volumen, así que pueden correr al mismo tiempo en una máquina:

```bash
docker compose --env-file env/qa.env up -d --build
```

En producción las credenciales llegan solo por variables de entorno (`env/prod.env` está en `.gitignore`), el detalle de los errores no se expone y Swagger UI está apagado.

Además existe el perfil `local` (H2 en memoria) para correr sin Docker y el perfil `test` para las pruebas.

---

## 3. Arquitectura

**Arquitectura hexagonal (puertos y adaptadores)** con las capas exigidas: *entity*, *repository*, *service* y *controller*.

```
backend/src/main/java/com/ptrinity
├── domain                      Núcleo: no depende de Spring ni de JPA
│   ├── model                   Cliente, Cuenta, Movimiento, EstadoDeCuenta (reglas de negocio)
│   ├── exception               ReglaNegocio, RecursoNoEncontrado, RecursoDuplicado
│   └── port
│       ├── in                  Casos de uso: GestionarClientes, GestionarCuentas, RealizarTransacciones
│       └── out                 Repositorios y generador de números de cuenta
├── application/service         ClienteService, CuentaService, TransaccionService (orquestan y abren transacción)
└── infrastructure
    ├── persistence             Adaptadores de salida
    │   ├── entity              Entidades JPA
    │   ├── repository          Repositorios Spring Data
    │   └── adapter             Implementan los puertos de salida + mapper
    ├── rest                    Adaptadores de entrada
    │   ├── controller          Controladores REST
    │   ├── dto                 Request/Response con Bean Validation
    │   └── error               Manejo global de errores (RFC 9457)
    └── config                  Reloj, OpenAPI
```

Flujo de una petición: `Controller → puerto in (caso de uso) → Service → dominio → puerto out → Adapter → JPA → PostgreSQL`.

El dominio no conoce la base de datos ni HTTP: se puede probar con pruebas unitarias puras y cambiar PostgreSQL por otro motor tocando solo los adaptadores.

**Frontend**: SPA en React organizada en componentes (`ClientesPanel`, `ClienteForm`, `CuentasPanel`, `TransaccionesPanel`, `EstadoCuentaPanel`, `TablaMovimientos`, `Aviso`) y un único módulo `api.ts` que conoce las URLs del backend. Nginx sirve el front y enruta `/api` al backend en el mismo origen, sin CORS.

**Componentes desplegables**: `db` (PostgreSQL), `backend` (Spring Boot) y `frontend` (Nginx), cada uno en su contenedor.

---

## 4. Patrones de diseño

| Patrón | Dónde | Para qué |
|---|---|---|
| Puertos y adaptadores (Hexagonal) | `domain/port`, `infrastructure/*/adapter` | Aislar el negocio de la tecnología |
| Repository | `*RepositoryPort`, `*JpaRepository` | Abstraer el acceso a datos |
| Adapter | `ClientePersistenceAdapter`, `CuentaPersistenceAdapter`, ... | Traducir puertos del dominio a Spring Data |
| Data Mapper | `PersistenciaMapper` | Separar el modelo de dominio de las entidades JPA |
| DTO | `infrastructure/rest/dto` | Contrato de la API independiente del dominio |
| Static Factory Method | `Cliente.registrar`, `Cuenta.abrir`, `Movimiento.nuevo` | Crear objetos siempre válidos |
| Rich Domain Model | `Cuenta.debitar`, `Cuenta.cancelar`, ... | Las reglas viven en el objeto que tiene los datos |
| Strategy | `GeneradorNumeroCuentaPort` | Cambiar cómo se generan los números sin tocar el servicio |
| Dependency Injection | Constructores de servicios y adaptadores | Bajo acoplamiento y pruebas con mocks |
| Controller Advice | `ManejadorErrores` | Un solo lugar para traducir excepciones a HTTP |
| Facade (frontend) | `api.ts` | Un punto de acceso a la API para todos los componentes |

---

## 5. Principios SOLID

- **S – Responsabilidad única**: cada clase tiene un motivo de cambio. `ClienteService` orquesta casos de uso de clientes; `ManejadorErrores` solo traduce errores; `PersistenciaMapper` solo convierte objetos.
- **O – Abierto/cerrado**: un nuevo generador de números de cuenta o un nuevo motor de base de datos se agregan como nuevos adaptadores, sin modificar los servicios.
- **L – Sustitución de Liskov**: cualquier implementación de `CuentaRepositoryPort` (JPA o un mock de prueba) sirve al servicio sin cambiar su comportamiento; las pruebas unitarias de servicios lo demuestran.
- **I – Segregación de interfaces**: tres puertos de entrada pequeños (`GestionarClientesUseCase`, `GestionarCuentasUseCase`, `RealizarTransaccionesUseCase`) y un puerto de salida por agregado, en vez de una interfaz gigante.
- **D – Inversión de dependencias**: los servicios dependen de interfaces del dominio (`port/out`), no de Spring Data. Los controladores dependen de los casos de uso (`port/in`), no de las clases concretas.

---

## 6. Principios ACID

Cada operación de dinero se ejecuta en **una sola transacción de base de datos** (`@Transactional` en `TransaccionService`):

- **Atomicidad**: en una transferencia el débito, el crédito y los dos movimientos se guardan juntos o no se guarda nada. Si el destino no existe o el saldo no alcanza, se revierte todo (prueba `transferenciaFallidaNoMueveSaldos`).
- **Consistencia**: las reglas se validan en el dominio y, además, en la base de datos con restricciones `CHECK`, `UNIQUE` y `FOREIGN KEY` (por ejemplo, una cuenta de ahorros nunca puede quedar con saldo negativo ni un cliente menor de edad puede existir, aunque se escriba directo en SQL).
- **Aislamiento**: las cuentas se leen con **bloqueo pesimista** (`SELECT ... FOR UPDATE`) antes de modificar el saldo, para que dos retiros simultáneos no gasten el mismo dinero. En las transferencias las cuentas se bloquean siempre en orden ascendente de id para evitar interbloqueos.
- **Durabilidad**: PostgreSQL confirma en disco (WAL) al hacer *commit*, y el volumen de Docker conserva los datos entre reinicios.

---

## 7. Base de datos (DDL y DML)

Scripts versionados con **Flyway**, que los ejecuta al arrancar:

| Script | Tipo | Ambientes |
|---|---|---|
| `backend/src/main/resources/db/migration/V1__esquema_inicial.sql` | DDL | Todos |
| `backend/src/main/resources/db/demo/V1_1__datos_demo.sql` | DML | dev, qa, local |

Modelo:

```
clientes 1 ──── * cuentas 1 ──── * movimientos
```

Reglas garantizadas por la base de datos:

| Restricción | Regla |
|---|---|
| `ck_clientes_mayor_edad` | Un cliente menor de edad no puede existir |
| `uk_clientes_identificacion` | No hay dos clientes con el mismo tipo y número de identificación |
| `ck_clientes_nombres`, `ck_clientes_apellido` | Mínimo 2 caracteres |
| `fk_cuentas_cliente ... ON DELETE RESTRICT` | Una cuenta solo existe vinculada a un cliente y un cliente con productos no se puede borrar |
| `uk_cuentas_numero`, `ck_cuentas_numero` | Número único de 10 dígitos; ahorros inicia en 53 y corriente en 33 |
| `ck_cuentas_saldo_ahorros` | La cuenta de ahorros no puede tener saldo menor a $0 |
| `ck_cuentas_cancelada_sin_saldo` | Solo se cancela una cuenta con saldo $0 |
| `ck_movimientos_coherencia` | Consignación es crédito, retiro es débito, transferencia tiene contraparte |

El DML de demostración crea 3 clientes, 3 cuentas y 6 movimientos con saldos coherentes (una prueba lo verifica).

---

## 8. Reglas de negocio implementadas

**Clientes**
- Atributos: id, tipo y número de identificación, nombres, apellido, correo, fecha de nacimiento, fecha de creación y fecha de modificación.
- La fecha de creación se calcula al registrar; la de modificación se recalcula en cada actualización.
- No se registra ni se actualiza a un menor de edad (dominio + `CHECK` en BD).
- No se elimina un cliente con productos vinculados.
- Opcionales implementados: correo con formato `xxxx@xxxxx.xxx`; nombres y apellido de al menos 2 caracteres.

**Productos (cuentas)**
- Solo cuenta de ahorros o corriente, siempre vinculada a un cliente.
- Atributos: id, tipo, número, estado (activa, inactiva, cancelada), saldo, exenta GMF, fecha de creación, fecha de modificación y cliente.
- Número único de 10 dígitos generado automáticamente: ahorros `53XXXXXXXX`, corriente `33XXXXXXXX`.
- Toda cuenta nueva queda **activa** con saldo $0.
- Se activa o inactiva en cualquier momento; solo se cancela con saldo $0; una cuenta cancelada no vuelve a activarse.
- Ahorros nunca queda en negativo.

**Transacciones**
- Solo consignación, retiro y transferencia; solo sobre cuentas activas.
- Cada transacción exitosa actualiza el saldo y el saldo disponible.
- La transferencia exige cuentas existentes y distintas, y genera el débito en la cuenta de envío y el crédito en la de recepción, enlazados por la misma referencia.
- Los movimientos son inmutables (libro contable): se crean y se consultan, no se editan ni borran.

**Supuestos documentados** (no los define el enunciado):
- *Saldo disponible*: en ahorros es el saldo; en corriente es el saldo más un cupo de sobregiro de $1.000.000.
- *GMF (4 x 1000)*: las cuentas no exentas pagan 0,4 % sobre cada débito (retiro o transferencia enviada).
- *Eliminar una cuenta*: `DELETE /api/cuentas/{id}` hace un borrado lógico (la cancela), para conservar el historial de movimientos.

---

## 9. API REST

Documentación interactiva en **Swagger UI**: `/swagger-ui.html` (especificación en `/v3/api-docs`).

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/clientes` | Registrar cliente |
| GET | `/api/clientes` | Listar clientes |
| GET | `/api/clientes/{id}` | Consultar cliente |
| PUT | `/api/clientes/{id}` | Actualizar cliente |
| DELETE | `/api/clientes/{id}` | Eliminar cliente |
| POST | `/api/cuentas` | Abrir cuenta |
| GET | `/api/cuentas?clienteId=` | Listar cuentas |
| GET | `/api/cuentas/{id}` | Consultar cuenta |
| PUT | `/api/cuentas/{id}` | Cambiar exención de GMF |
| PATCH | `/api/cuentas/{id}/estado` | Activar, inactivar o cancelar |
| DELETE | `/api/cuentas/{id}` | Cancelar cuenta (borrado lógico) |
| GET | `/api/cuentas/{id}/estado-de-cuenta?desde=&hasta=` | Estado de cuenta |
| POST | `/api/transacciones/consignaciones` | Consignar |
| POST | `/api/transacciones/retiros` | Retirar |
| POST | `/api/transacciones/transferencias` | Transferir |
| GET | `/api/transacciones?cuentaId=` | Movimientos de una cuenta |
| GET | `/api/transacciones/{id}` | Consultar movimiento |

Errores en formato *Problem Details* (RFC 9457):

| Código | Caso |
|---|---|
| 400 | Datos inválidos (incluye `errores` por campo) o JSON mal formado |
| 404 | Recurso inexistente |
| 409 | Identificación duplicada o violación de integridad |
| 422 | Regla de negocio (menor de edad, saldo insuficiente, cuenta inactiva, cliente con productos...) |

---

## 10. Pruebas

| Tipo | Clases | Qué cubren |
|---|---|---|
| Unitarias de dominio | `ClienteTest`, `CuentaTest`, `EstadoDeCuentaTest` | Todas las reglas de negocio |
| Unitarias de servicios (Mockito) | `ClienteServiceTest`, `CuentaServiceTest`, `TransaccionServiceTest` | Orquestación, duplicados, bloqueo y atomicidad |
| Controladores (MockMvc) | `ClienteControllerTest`, `CuentaControllerTest`, `TransaccionControllerTest` | Contrato HTTP, validación y códigos de error |
| Persistencia (`@DataJpaTest`) | `PersistenciaAdapterTest`, `DatosDemoTest` | Adaptadores, restricciones del DDL y coherencia del DML |
| Extremo a extremo | `FlujoCompletoTest` | Flujo completo, Swagger y Actuator |
| Componentes React | `*.test.tsx` | Interacción de cada pantalla con la API simulada |

---

## 11. Servicios en la nube

| Servicio | Uso |
|---|---|
| **GitHub** | Repositorio único y control de versiones |
| **GitHub Actions** | Pipeline CI/CD (`.github/workflows/ci.yml`): pruebas, umbral de cobertura del 85 %, validación del flujo de ramas y publicación de imágenes |
| **GitHub Container Registry (GHCR)** | Imágenes `ptrinity-backend` y `ptrinity-frontend` etiquetadas por ambiente: `dev`, `qa`, `latest` (main), la versión (`v1.0.0`) y el commit |

Cualquier servidor con Docker puede desplegar un ambiente descargando las imágenes de GHCR. Las imágenes son portables a un servicio administrado de contenedores (AWS ECS/Fargate, Azure Container Apps o Google Cloud Run) con una base PostgreSQL administrada (RDS, Azure Database o Cloud SQL), cambiando solo las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

---

## 12. Control de versiones (Git Flow)

```
feature/* ──► dev ──► qa ──► main ──► tag vX.Y.Z
                         ▲
hotfix/* ────────────────┘ (y de vuelta a dev)
```

| Rama | Propósito |
|---|---|
| `main` | Producción. Solo recibe merges desde `qa` (o `hotfix/*`) y cada versión lleva un tag |
| `qa` | Pruebas de calidad. Recibe la integración aprobada de `dev` |
| `dev` | Integración continua del desarrollo |
| `feature/*` | Una funcionalidad por rama, se integra a `dev` con `merge --no-ff` |
| `hotfix/*` | Correcciones urgentes sobre producción |

Ramas de funcionalidad usadas: `feature/backend-base`, `feature/dominio`, `feature/persistencia`, `feature/servicios`, `feature/api-rest`, `feature/frontend`, `feature/docker-ci` y `feature/docs`.

Los mensajes siguen *Conventional Commits* (`feat`, `fix`, `test`, `build`, `ci`, `docs`, `chore`). El pipeline rechaza pull requests que no respeten el flujo (por ejemplo, de `feature/*` directo a `main`).
