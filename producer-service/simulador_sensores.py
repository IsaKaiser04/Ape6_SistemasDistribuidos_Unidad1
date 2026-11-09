"""Producer: simula 3 vehiculos x 3 sensores y publica en MQTT (flota/{vehiculo}/{sensor})."""
import argparse
import json
import os
import random
import time
from datetime import datetime

import paho.mqtt.client as mqtt

BROKER = os.environ.get("MQTT_BROKER", "localhost")
PUERTO = int(os.environ.get("MQTT_PORT", "1883"))
VEHICULOS = ["VH-001", "VH-002", "VH-003"]
SENSORES = ("gps", "temperatura", "combustible")


def crear_cliente():
    """Compatible con paho-mqtt 1.x y 2.x."""
    try:
        return mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    except AttributeError:
        return mqtt.Client()


def on_connect(client, userdata, flags, rc, *args):
    print(f"[{datetime.now().isoformat()}] Conectado al broker MQTT {BROKER}:{PUERTO} (rc={rc})", flush=True)


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


def principal(rondas, pausa):
    client = crear_cliente()
    client.on_connect = on_connect
    # connect_async + loop_start: si Mosquitto aun no esta arriba, reintenta solo
    client.connect_async(BROKER, PUERTO)
    client.loop_start()
    print("Iniciando simulador de sensores IoT...", flush=True)
    ronda = 0
    try:
        while rondas == 0 or ronda < rondas:  # --rondas 0 = infinito
            ronda += 1
            for vehiculo in VEHICULOS:
                ts = datetime.now().isoformat()
                for sensor in SENSORES:
                    payload = {"vehicle_id": vehiculo, "timestamp": ts}
                    payload.update(SIMULADORES[sensor](vehiculo))
                    client.publish(f"flota/{vehiculo}/{sensor}", json.dumps(payload), qos=1)
                print(f"[{ts}] Datos publicados para {vehiculo}", flush=True)
                time.sleep(pausa)
    except KeyboardInterrupt:
        print("Deteniendo simulador...")
    finally:
        time.sleep(0.5)  # deja salir los ultimos mensajes QoS 1
        client.loop_stop()
        client.disconnect()


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--rondas", type=int, default=int(os.environ.get("SIM_RONDAS", "0")),
                    help="numero de rondas (3 vehiculos x 3 sensores = 9 mensajes); 0 = infinito")
    ap.add_argument("--pausa", type=float, default=float(os.environ.get("SIM_PAUSA", "5")),
                    help="segundos de espera despues de cada vehiculo")
    args = ap.parse_args()
    principal(args.rondas, args.pausa)
