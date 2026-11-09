"""Producer (puente): MQTT -> RabbitMQ. Reenvia todo el GPS y solo las alertas."""
import json
import os
import time

import paho.mqtt.client as mqtt
import pika

MQTT_BROKER = os.environ.get("MQTT_BROKER", "localhost")
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.environ.get("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.environ.get("RABBITMQ_USER", "flota")
RABBITMQ_PASS = os.environ.get("RABBITMQ_PASS", "flota123")

COLA_GPS = "cola.gps.telemetria"
COLA_TEMP = "cola.alertas.temperatura"
COLA_FUEL = "cola.combustible.nivel"

UMBRAL_TEMP = 4    # alerta si temperature > 4
UMBRAL_FUEL = 20   # alerta si fuel_level < 20


def enrutar(topic, payload):
    """Devuelve (cola, mensaje) si el dato debe ir a RabbitMQ, o None si se descarta."""
    if topic.endswith("/gps"):
        return COLA_GPS, payload
    if topic.endswith("/temperatura") and payload["temperature"] > UMBRAL_TEMP:
        return COLA_TEMP, {**payload, "type": "TEMP_ALERT", "message": "Temperatura excedida"}
    if topic.endswith("/combustible") and payload["fuel_level"] < UMBRAL_FUEL:
        return COLA_FUEL, {**payload, "type": "FUEL_LOW", "message": "Combustible bajo"}
    return None


class PublicadorRabbit:
    """Conexion a RabbitMQ con reintento: sobrevive a que RabbitMQ arranque despues
    o a que la red (hotspot) se corte un momento."""

    def __init__(self):
        self.conexion = None
        self.canal = None

    def conectar(self):
        credenciales = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
        params = pika.ConnectionParameters(
            host=RABBITMQ_HOST, port=RABBITMQ_PORT, credentials=credenciales)
        while True:
            try:
                self.conexion = pika.BlockingConnection(params)
                self.canal = self.conexion.channel()
                for cola in (COLA_GPS, COLA_TEMP, COLA_FUEL):
                    self.canal.queue_declare(queue=cola, durable=True)
                print(f"Conectado a RabbitMQ {RABBITMQ_HOST}:{RABBITMQ_PORT}; colas listas", flush=True)
                return
            except Exception as e:
                print(f"RabbitMQ no disponible en {RABBITMQ_HOST}:{RABBITMQ_PORT} ({e}). Reintentando en 3 s...",
                      flush=True)
                time.sleep(3)

    def publicar(self, cola, mensaje):
        cuerpo = json.dumps(mensaje)
        props = pika.BasicProperties(delivery_mode=2, content_type="application/json")  # persistente
        try:
            self.canal.basic_publish(exchange="", routing_key=cola, body=cuerpo, properties=props)
        except Exception:
            # conexion caida: reconectar y reintentar una vez
            self.conectar()
            self.canal.basic_publish(exchange="", routing_key=cola, body=cuerpo, properties=props)

    def cerrar(self):
        try:
            if self.conexion and self.conexion.is_open:
                self.conexion.close()
        except Exception:
            pass


def crear_cliente(userdata):
    try:
        return mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, userdata=userdata)
    except AttributeError:
        return mqtt.Client(userdata=userdata)


def on_connect(client, userdata, flags, rc, *args):
    client.subscribe("flota/#", qos=1)
    print(f"Bridge MQTT-RabbitMQ iniciado (MQTT {MQTT_BROKER}:{MQTT_PORT})...", flush=True)


def on_message(client, userdata, msg):
    try:
        payload = json.loads(msg.payload.decode("utf-8"))
        destino = enrutar(msg.topic, payload)
        if destino is None:
            return
        cola, mensaje = destino
        userdata["rabbit"].publicar(cola, mensaje)
        print(f"-> {cola}: {payload.get('vehicle_id')}", flush=True)
    except Exception as e:
        print(f"Error en puente ({msg.topic}): {e}", flush=True)


def principal():
    rabbit = PublicadorRabbit()
    rabbit.conectar()
    client = crear_cliente({"rabbit": rabbit})
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect_async(MQTT_BROKER, MQTT_PORT)
    try:
        client.loop_forever(retry_first_connection=True)
    except KeyboardInterrupt:
        print("Deteniendo bridge...")
    finally:
        client.disconnect()
        rabbit.cerrar()


if __name__ == "__main__":
    principal()
