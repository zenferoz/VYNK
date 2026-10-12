# VYNK AI Semantic Inference Microservice (Phase 4)
FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy service logic
COPY server.py .

# Bind to 0.0.0.0 so container is reachable within the Docker network
ENV VYNK_INFERENCE_HOST=0.0.0.0
ENV VYNK_INFERENCE_PORT=8001

EXPOSE 8001

HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/health')" || exit 1

CMD ["python", "server.py"]
