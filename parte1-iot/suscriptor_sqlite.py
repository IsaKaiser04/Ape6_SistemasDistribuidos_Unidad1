import json
import sqlite3
from datetime import datetime

import paho.mqtt.client as mqtt

BROKER = "localhost"
PUERTO = 1883
DB = "telemetria.db"
UMBRAL_TEMP = 4    # alerta si temperature > 4
UMBRAL_FUEL = 20   # alerta si fuel_level < 20

TOPICS = ["flota/+/gps", "flota/+/temperatura", "flota/+/combustible"]


def inicializar_base_datos():
    conn = sqlite3.connect(DB)
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS gps_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        vehicle_id TEXT, lat REAL, lng REAL, speed REAL, timestamp TEXT)""")
    cur.execute("""CREATE TABLE IF NOT EXISTS temp_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        vehicle_id TEXT, temperature REAL, unit TEXT, timestamp TEXT)""")
    cur.execute("""CREATE TABLE IF NOT EXISTS fuel_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        vehicle_id TEXT, fuel_level REAL, unit TEXT, timestamp TEXT)""")
    conn.commit()
    conn.close()
    print("Base de datos inicializada.")


def procesar_mensaje(topic, payload, db=DB):
    """Guarda el dato en SQLite (siempre) y devuelve el texto de alerta, o None."""
    alerta = None
    conn = sqlite3.connect(db)
    cur = conn.cursor()
    if topic.endswith("/gps"):
        cur.execute(
            "INSERT INTO gps_data (vehicle_id, lat, lng, speed, timestamp) VALUES (?,?,?,?,?)",
            (payload["vehicle_id"], payload["lat"], payload["lng"],
             payload["speed"], payload["timestamp"]))
    elif topic.endswith("/temperatura"):
        if payload["temperature"] > UMBRAL_TEMP:
            alerta = f"ALERTA: {payload['vehicle_id']} temperatura alta ({payload['temperature']} C)"
        cur.execute(
            "INSERT INTO temp_data (vehicle_id, temperature, unit, timestamp) VALUES (?,?,?,?)",
            (payload["vehicle_id"], payload["temperature"],
             payload["unit"], payload["timestamp"]))
    elif topic.endswith("/combustible"):
        if payload["fuel_level"] < UMBRAL_FUEL:
            alerta = f"ALERTA: {payload['vehicle_id']} combustible bajo ({payload['fuel_level']} %)"
        cur.execute(
            "INSERT INTO fuel_data (vehicle_id, fuel_level, unit, timestamp) VALUES (?,?,?,?)",
            (payload["vehicle_id"], payload["fuel_level"],
             payload["unit"], payload["timestamp"]))
    conn.commit()
    conn.close()
    return alerta


def crear_cliente():
    try:
        return mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    except AttributeError:
        return mqtt.Client()


def on_connect(client, userdata, flags, rc, *args):
    # Suscribir en on_connect: si el cliente reconecta, se vuelve a suscribir
    for t in TOPICS:
        client.subscribe(t, qos=1)
    print("Suscriptor activo. Escuchando topics flota/+/...")


def on_message(client, userdata, msg):
    try:
        payload = json.loads(msg.payload.decode("utf-8"))
        alerta = procesar_mensaje(msg.topic, payload)
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg.topic} guardado")
        if alerta:
            print(alerta)
    except Exception as e:  # un mensaje malo no debe tumbar el suscriptor
        print(f"Error procesando {msg.topic}: {e}")


def principal():
    inicializar_base_datos()
    client = crear_cliente()
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(BROKER, PUERTO)
    try:
        client.loop_forever()
    except KeyboardInterrupt:
        print("Deteniendo suscriptor...")
        client.disconnect()


if __name__ == "__main__":
    principal()
