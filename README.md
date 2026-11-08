# Monitoreo de Flota Logística con MQTT, RabbitMQ y Spring Boot

**Asignatura:** Sistemas Distribuidos – 6to ciclo, Carrera de Computación (FEIRNNR, UNL)
**Docente:** José O. Guamán Q.
**Prácticas:** Nro. 5 (Parte 1: capa IoT) y Nro. 6 (Parte 2: microservicios)

**Integrantes:**
- Marco Castillo
- Viviana Córdova
- Isauro Rivera
- Ariel Tandazo

## 1. Descripción

Sistema de mensajería distribuida para monitorear en tiempo real una flota de vehículos de reparto. Cada vehículo envía telemetría (GPS, temperatura de la carga y nivel de combustible) por **MQTT**. Los datos se almacenan, se detectan alertas por umbral y se reenvían a **RabbitMQ**, donde microservicios en **Spring Boot** los procesan y los exponen mediante una **API REST**.

## 2. Arquitectura

```
Simulador (3 vehículos x 3 sensores)
        │  MQTT (1883)
        ▼
Mosquitto ──► Suscriptor Python ──► SQLite (telemetria.db) + alertas
        │
        ▼  flota/#
Puente Python (MQTT → RabbitMQ)
        │  AMQP (5672)
        ▼
RabbitMQ: cola.gps.telemetria | cola.alertas.temperatura
          cola.combustible.nivel | cola.notificaciones
        │
        ▼
Spring Boot (fleet-monitor): consumidores + H2 + API REST (8080)
```

- **Topics MQTT:** `flota/{vehiculo_id}/{gps|temperatura|combustible}`
- **Umbrales de alerta:** temperatura > 4 °C y combustible < 20 %
- **Puente:** reenvía todo el GPS y solo las alertas de temperatura y combustible, con colas duraderas y mensajes persistentes.
- **Spring Boot:** el servicio de alertas reenvía cada alerta a `cola.notificaciones`.

## 3. Estructura del repositorio

```
├── README.md
├── .gitignore
├── parte1-iot/
│   ├── docker-compose.yml          # Mosquitto + RabbitMQ
│   ├── mosquitto/config/mosquitto.conf
│   ├── simulador_sensores.py
│   ├── suscriptor_sqlite.py
│   ├── mqtt_rabbitmq_bridge.py
│   ├── prueba_alertas.py
│   ├── verificar_resultados.py
│   ├── requirements.txt
│   └── respuestas_control.md
├── parte2-springboot/
│   ├── respuestas_control_parte2.md
│   └── fleet-monitor/              # Proyecto Maven (Spring Boot 3.5, Java 17)
└── evidencias/                     # Capturas de pantalla
```

## 4. Requisitos

- Docker y Docker Compose
- Python 3.10 o superior (paho-mqtt y pika)
- Java JDK 17 y Maven 3.9 o superior

## 5. Ejecución

**Importante:** deben estar detenidos el Mosquitto y el RabbitMQ instalados en el sistema (puertos 1883, 5672 y 15672 libres).

### Parte 1: capa IoT

```bash
cd parte1-iot
pip install -r requirements.txt
docker compose up -d                       # Mosquitto y RabbitMQ
```

Luego, cada uno en su terminal y en este orden:

```bash
python3 suscriptor_sqlite.py               # guarda en SQLite y muestra alertas
python3 mqtt_rabbitmq_bridge.py            # puente MQTT -> RabbitMQ
python3 simulador_sensores.py --rondas 2   # publica la telemetría
python3 verificar_resultados.py            # al terminar: conteo y alertas
```

Para forzar alertas: `python3 prueba_alertas.py`.

### Parte 2: microservicios Spring Boot

```bash
cd parte2-springboot/fleet-monitor
mvn spring-boot:run
```

### API REST (puerto 8080)

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/fleet/status` | Resumen: vehículos, registros GPS y alertas |
| GET | `/api/fleet/vehicle/{id}/telemetria` | Últimos 20 registros GPS del vehículo |
| GET | `/api/fleet/alerts` | Últimas 20 alertas |

```bash
curl http://localhost:8080/api/fleet/status
curl http://localhost:8080/api/fleet/vehicle/VH-001/telemetria
curl http://localhost:8080/api/fleet/alerts
```

## 6. Credenciales y puertos

| Servicio | Puerto | Acceso |
|---|---|---|
| Mosquitto (MQTT) | 1883 | anónimo (solo laboratorio local) |
| Mosquitto (WebSocket) | 9001 | anónimo |
| RabbitMQ (AMQP) | 5672 | usuario `flota` / contraseña `flota123` |
| RabbitMQ (panel web) | 15672 | `http://localhost:15672` (`flota` / `flota123`) |
| Spring Boot | 8080 | API REST |
| Consola H2 | 8080 | `/h2-console`, JDBC `jdbc:h2:mem:fleetdb`, usuario `sa` |

Se usó el usuario `flota` en lugar de `guest` o `admin` para evitar conflictos con otros proyectos que usan RabbitMQ en el mismo equipo. Estas credenciales son solo para el laboratorio local.

## 7. Decisiones técnicas

- **Content-type:** el puente en Python publica sin content-type. Por eso los consumidores de Spring reciben el `Message` completo y leen el cuerpo como UTF-8.
- **Nombres de campos:** el simulador usa `vehicle_id` (snake_case), así que el modelo Java lo mapea con `@JsonProperty`.
- **Persistencia:** la Parte 1 guarda en SQLite y la Parte 2 en H2 en memoria, que se pierde al reiniciar Spring.
- **Exchange:** `exchange.fleet` y sus bindings están declarados, pero el puente publica por el exchange por defecto con la routing key igual al nombre de la cola, así que los consumidores leen directamente de las colas.
- **Manejo de errores:** los consumidores capturan las excepciones para que un mensaje malformado no se reencole indefinidamente.

## 8. Problemas encontrados y solución

| Problema | Causa | Solución |
|---|---|---|
| `address already in use` en 1883 | Mosquitto local activo en el sistema | `sudo systemctl stop mosquitto` y `disable` |
| `ACCESS_REFUSED` (403) en RabbitMQ | Puerto 5672 ocupado por otra instancia o usuario `guest` rechazado | Detener la otra instancia y definir el usuario `flota` en el compose |
| Puertos ocupados sin contenedores | Procesos `docker-proxy` huérfanos | `sudo pkill docker-proxy` y volver a levantar |


## 9. Evidencias

Las capturas están en la carpeta [`evidencias/`](evidencias/): contenedores activos, publicación en `flota/#`, SQLite, alertas, colas de RabbitMQ y respuestas de la API REST.

## 10. Bibliografía

- J. Guamán, _Guía de Actividades Práctico-Experimentales Nro. 005 y 006_, Universidad Nacional de Loja, 2026.
