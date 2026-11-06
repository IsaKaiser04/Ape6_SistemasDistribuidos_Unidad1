import argparse
import json
import random
import time
from datetime import datetime

import paho.mqtt.client as mqtt

BROKER = "localhost"
PUERTO = 1883
VEHICULOS = ["VH-001", "VH-002", "VH-003"]
SENSORES = ("gps", "temperatura", "combustible")
PAUSA = 5  # segundos despues de cada vehiculo (segun la guia)


def crear_cliente():
    """Compatible con paho-mqtt 1.x y 2.x."""
    try:
        return mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    except AttributeError:
        return mqtt.Client()


def on_connect(client, userdata, flags, rc, *args):
    print(f"[{datetime.now().isoformat()}] Conectado al broker MQTT (rc={rc})")


def simular_gps(id_vehiculo):
    base_lat, base_lng = -2.1709, -79.9224
    return {
        "lat": round(base_lat + random.uniform(-0.01, 0.01), 6),
        "lng": round(base_lng + random.uniform(-0.01, 0.01), 6),
        "speed": round(random.uniform(20, 80), 1),
    }


def simular_temperatura(id_vehiculo):
    return {"temperature": round(random.uniform(-5, 8), 1), "unit": "celsius"}


def simular_combustible(id_vehiculo):
    return {"fuel_level": round(random.uniform(10, 100), 1), "unit": "percent"}


SIMULADORES = {
    "gps": simular_gps,
    "temperatura": simular_temperatura,
    "combustible": simular_combustible,
}


def principal(rondas):
    client = crear_cliente()
    client.on_connect = on_connect
    client.connect(BROKER, PUERTO)
    client.loop_start()
    print("Iniciando simulador de sensores IoT...")
    ronda = 0
    try:
        while rondas == 0 or ronda < rondas:  # --rondas 0 = infinito
            ronda += 1
            for vehiculo in VEHICULOS:
                ts = datetime.now().isoformat()
                for sensor in SENSORES:
                    payload = {"vehicle_id": vehiculo, "timestamp": ts}
                    payload.update(SIMULADORES[sensor](vehiculo))
                    topic = f"flota/{vehiculo}/{sensor}"
                    client.publish(topic, json.dumps(payload), qos=1)
                print(f"[{ts}] Datos publicados para {vehiculo}")
                time.sleep(PAUSA)
    except KeyboardInterrupt:
        print("Deteniendo simulador...")
    finally:
        time.sleep(0.5)  # deja salir los ultimos mensajes QoS 1
        client.loop_stop()
        client.disconnect()


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--rondas", type=int, default=0,
                    help="numero de rondas (3 vehiculos x 3 sensores = 9 mensajes); 0 = infinito")
    principal(ap.parse_args().rondas)
