import json

import paho.mqtt.client as mqtt
import pika

MQTT_BROKER = "localhost"
MQTT_PORT = 1883
RABBITMQ_HOST = "localhost"
RABBITMQ_USER = "flota"
RABBITMQ_PASS = "flota123"

COLA_GPS = "cola.gps.telemetria"
COLA_TEMP = "cola.alertas.temperatura"
COLA_FUEL = "cola.combustible.nivel"


def enrutar(topic, payload):
    """Devuelve (cola, mensaje) si el dato debe ir a RabbitMQ, o None si se descarta."""
    if topic.endswith("/gps"):
        return COLA_GPS, payload
    if topic.endswith("/temperatura") and payload["temperature"] > 4:
        return COLA_TEMP, {**payload, "type": "TEMP_ALERT", "message": "Temperatura excedida"}
    if topic.endswith("/combustible") and payload["fuel_level"] < 20:
        return COLA_FUEL, {**payload, "type": "FUEL_LOW", "message": "Combustible bajo"}
    return None


def configurar_rabbitmq():
    credenciales = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
    conexion = pika.BlockingConnection(
        pika.ConnectionParameters(host=RABBITMQ_HOST, credentials=credenciales))
    canal = conexion.channel()
    for cola in (COLA_GPS, COLA_TEMP, COLA_FUEL):
        canal.queue_declare(queue=cola, durable=True)
    print("Colas RabbitMQ creadas exitosamente")
    return conexion, canal


def crear_cliente(userdata):
    try:
        return mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, userdata=userdata)
    except AttributeError:
        return mqtt.Client(userdata=userdata)


def on_connect(client, userdata, flags, rc, *args):
    client.subscribe("flota/#", qos=1)
    print("Bridge MQTT-RabbitMQ iniciado...")


def on_message(client, userdata, msg):
    try:
        payload = json.loads(msg.payload.decode("utf-8"))
        destino = enrutar(msg.topic, payload)
        if destino is None:
            return
        cola, mensaje = destino
        userdata["canal"].basic_publish(
            exchange="",            # exchange predeterminado
            routing_key=cola,       # = nombre de la cola
            body=json.dumps(mensaje),
            properties=pika.BasicProperties(delivery_mode=2))  # persistente
        print(f"-> {cola}: {payload.get('vehicle_id')}")
    except Exception as e:
        print(f"Error en puente ({msg.topic}): {e}")


def principal():
    conexion, canal = configurar_rabbitmq()
    client = crear_cliente({"canal": canal})
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(MQTT_BROKER, MQTT_PORT)
    try:
        client.loop_forever()
    except KeyboardInterrupt:
        print("Deteniendo bridge...")
    finally:
        client.disconnect()
        conexion.close()


if __name__ == "__main__":
    principal()
