FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY simulador_sensores.py mqtt_rabbitmq_bridge.py prueba_alertas.py ./
CMD ["python", "-u", "simulador_sensores.py"]
