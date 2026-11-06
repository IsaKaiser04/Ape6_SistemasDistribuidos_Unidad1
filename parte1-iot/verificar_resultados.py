import sqlite3

conn = sqlite3.connect("telemetria.db")
cur = conn.cursor()
print("=== Registros por tabla ===")
for tabla in ("gps_data", "temp_data", "fuel_data"):
    n = cur.execute(f"SELECT COUNT(*) FROM {tabla}").fetchone()[0]
    print(f"{tabla}: {n}")

print("\n=== Alertas de temperatura (> 4) ===")
for r in cur.execute("SELECT vehicle_id, temperature, timestamp FROM temp_data WHERE temperature > 4"):
    print(r)

print("\n=== Alertas de combustible (< 20) ===")
for r in cur.execute("SELECT vehicle_id, fuel_level, timestamp FROM fuel_data WHERE fuel_level < 20"):
    print(r)
conn.close()
