# Parte 2 – Microservicios distribuidos (4 integrantes, 4 servicios)

Reestructura de `parte2-springboot/fleet-monitor` (monolito) en **cuatro microservicios**, uno por integrante, que se conectan por red (hotspot de una laptop). `parte1-iot/` no se toca.

## 1. Arquitectura

```
 Laptop A: PRODUCER               Laptop B: PROCESSOR                 Laptop C: STORAGE
┌───────────────────────┐   ┌──────────────────────────────┐   ┌───────────────────────┐
│ simulador_sensores.py │   │ RabbitMQ (5672 / 15672)      │   │ storage-service :8083 │
│   │ MQTT              │   │   cola.gps.telemetria        │   │  consume              │
│   ▼                   │   │   cola.alertas.temperatura   │   │   cola.storage.gps    │
│ Mosquitto :1883       │   │   cola.combustible.nivel     │   │   cola.storage.alertas│
│   │ flota/#           │   │        │                     │   │  guarda en H2 (archivo)│
│   ▼                   │AMQP│       ▼                     │   │  API REST de consulta │
│ puente MQTT→RabbitMQ ─┼──►│ processor-service :8082      │   └──────────▲────────────┘
└───────────────────────┘   │  valida / normaliza / alerta │              │ AMQP
                            │  ├─► exchange.fleet ─────────┼──────────────┘
                            │  └─► cola.notificaciones     │
                            └──────────────▲───────────────┘
                                           │ HTTP (stats / health)
                          Laptop D: GATEWAY :8080  ◄──── clientes (curl / Postman / navegador)
                              /api/fleet/**  → storage-service
                              /api/processor/** → processor-service
                              /gateway/health
```

| Servicio | Tecnología | Responsabilidad | Puerto |
|---|---|---|---|
| **producer-service** | Python + Mosquitto | *Produce* la telemetría (MQTT) y la lleva a RabbitMQ (puente; solo GPS y alertas) | 1883 |
| **processor-service** | Spring Boot + RabbitMQ | *Procesa*: valida GPS, normaliza (snake_case→camelCase), construye alertas, las reenvía a `cola.notificaciones` y entrega todo a storage por `exchange.fleet` | 8082 |
| **storage-service** | Spring Boot + JPA + H2 | *Guarda* GPS y alertas (H2 en archivo, sobrevive al reinicio) y expone la consulta | 8083 |
| **gateway-service** | Spring Boot | Punto de entrada único: enruta y consulta el estado de los demás | 8080 |

**Reparto sugerido (RabbitMQ va con el processor):**

| Rol | Integrante | Carpeta |
|---|---|---|
| A – Producer | ______ | `producer-service/` |
| B – Processor (+ RabbitMQ) | ______ | `processor-service/` |
| C – Storage | ______ | `storage-service/` |
| D – Gateway (+ hotspot) | ______ | `gateway-service/` |

### Mejoras respecto al monolito
- El `exchange.fleet` ahora **se usa de verdad**: processor publica por él (`storage.gps`, `storage.alert`) y storage consume sus colas (en el monolito solo estaba declarado).
- Mensajes entre processor y storage en **JSON con content-type**; cada servicio tiene su propio modelo (no comparten clases).
- El puente Python **reintenta** la conexión a RabbitMQ/Mosquitto (importa para arranque en distintas laptops y cortes del hotspot).
- Todo se configura por **variables de entorno** (no hay `localhost` fijo).
- Storage guarda en **archivo** (`./data/fleetdb`) en lugar de memoria.

## 2. Configuración de red (hotspot)

1. La laptop D activa el hotspot; las otras 3 se conectan. Anoten la IP de cada una (`ipconfig` / `ip a`; en Windows suele ser `192.168.137.x`).
2. **Firewall**: permitir entrada en cada laptop a los puertos que expone:
   A: 1883 · B: 5672, 15672, 8082 · C: 8083 · D: 8080. En Windows, marcar la red del hotspot como *privada*.
3. Mantener las laptops enchufadas y sin suspensión.

Quién necesita la IP de quién:

| Servicio | Variable | Valor |
|---|---|---|
| producer (puente) | `RABBITMQ_HOST` | IP de B |
| storage | `RABBITMQ_HOST` | IP de B |
| gateway | `STORAGE_URL` | `http://IP_de_C:8083` |
| gateway | `PROCESSOR_URL` | `http://IP_de_B:8082` |

## 3. Arranque

**Orden:** B (RabbitMQ + processor) → C (storage) → A (producer) → D (gateway). El puente y los consumidores reintentan, así que el orden no es estricto.

### Con Docker (recomendado, en cada laptop)
```bash
cd <su-servicio>
cp .env.example .env        # editar IPs (B no necesita .env)
docker compose up --build -d
docker compose logs -f
```
Si algún equipo no tiene Docker, ver abajo.

### Sin Docker
```bash
# B: RabbitMQ en Docker (solo el broker) y el processor
cd processor-service && docker compose up -d rabbitmq && mvn spring-boot:run

# C: storage
cd storage-service && RABBITMQ_HOST=<IP_B> mvn spring-boot:run

# D: gateway
cd gateway-service && STORAGE_URL=http://<IP_C>:8083 PROCESSOR_URL=http://<IP_B>:8082 mvn spring-boot:run

# A: producer (Mosquitto en Docker + scripts Python)
cd producer-service && pip install -r requirements.txt
docker compose up -d mosquitto
RABBITMQ_HOST=<IP_B> python3 mqtt_rabbitmq_bridge.py      # terminal 1
python3 simulador_sensores.py --rondas 2                  # terminal 2
```
(En Windows PowerShell: `$env:RABBITMQ_HOST="<IP_B>"` antes del comando.)

**Todo en una sola máquina:** no definan nada; los valores por defecto son `localhost` (y `host.docker.internal` en los compose). Sirve de respaldo si falla el hotspot.

## 4. Pruebas (desde cualquier laptop, usando la IP de D)

```bash
curl http://<IP_D>:8080/gateway/health          # gateway, storage y processor deben salir UP
curl http://<IP_D>:8080/api/processor/stats     # contadores del processor
curl http://<IP_D>:8080/api/fleet/status        # vehículos, registros GPS, alertas
curl http://<IP_D>:8080/api/fleet/vehicle/VH-001/telemetria
curl http://<IP_D>:8080/api/fleet/alerts
```
Forzar alertas: en A, `python3 prueba_alertas.py` (1 GPS + 1 temperatura 7 °C + 1 combustible 15 %).
Panel RabbitMQ: `http://<IP_B>:15672` (`flota` / `flota123`): ver colas `cola.storage.*` y `cola.notificaciones`.

## 5. Problemas típicos

| Síntoma | Causa probable |
|---|---|
| `/gateway/health` marca DOWN | IP mal puesta o firewall en esa laptop |
| Puente repite "RabbitMQ no disponible" | IP de B incorrecta, puerto 5672 bloqueado o RabbitMQ aún arrancando |
| `ACCESS_REFUSED` | usuario/clave distintos a `flota` / `flota123` |
| Storage sin datos pero processor sí cuenta | storage no llega a RabbitMQ (revisar `RABBITMQ_HOST`) |
| Las IP cambiaron | el hotspot se reinició; actualizar `.env` y reiniciar los servicios |
| Puertos ocupados | detener Mosquitto/RabbitMQ instalados en el sistema |
