"""Publica 3 mensajes de prueba: uno normal de GPS y dos que disparan alertas."""
import json
import time
from datetime import datetime

import paho.mqtt.client as mqtt

try:
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
except AttributeError:
    client = mqtt.Client()
client.connect("localhost", 1883)
client.loop_start()

ts = datetime.now().isoformat()
pruebas = [
    ("flota/VH-001/gps", {"vehicle_id": "VH-001", "timestamp": ts,
                          "lat": -2.1709, "lng": -79.9224, "speed": 42}),
    ("flota/VH-001/temperatura", {"vehicle_id": "VH-001", "timestamp": ts,
                                  "temperature": 7, "unit": "celsius"}),
    ("flota/VH-001/combustible", {"vehicle_id": "VH-001", "timestamp": ts,
                                  "fuel_level": 15, "unit": "percent"}),
]
for topic, payload in pruebas:
    client.publish(topic, json.dumps(payload), qos=1)
    print("Publicado:", topic)
time.sleep(1)
client.loop_stop()
client.disconnect()
