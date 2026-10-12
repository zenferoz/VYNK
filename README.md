# VYNK — Campus Student Networking Platform

A purpose-driven campus networking platform connecting students through skills, academic goals, and mutual introductions.

---

## Architecture & Monorepo Structure

```text
VYNK/
├── backend/            # Spring Boot 4 / Java 21 REST API
├── frontend/           # React 19 / Vite 8 Single Page Application
├── inference-service/  # Python 3.11 semantic similarity microservice
└── docker-compose.yml  # Local multi-container orchestration
```

---

## Production Deployment Guide

### 1. Database: Aiven MySQL
- Create a MySQL 8 instance on Aiven.
- Use the secure SSL JDBC connection string:
  `jdbc:mysql://<AIVEN_HOST>:<PORT>/defaultdb?sslMode=REQUIRED`
- Tables and schemas are automatically created via Flyway migrations (`V1.0.0` and `V1.0.1`).

### 2. Backend: Railway
- Connect this repository to Railway.
- **Root Directory:** `backend`
- **Builder:** Dockerfile (uses `backend/Dockerfile`)
- **Environment Variables:**
  - `PORT`: (Provided dynamically by Railway)
  - `DB_URL`: `jdbc:mysql://<AIVEN_HOST>:<PORT>/defaultdb?sslMode=REQUIRED`
  - `DB_USERNAME`: `<AIVEN_USER>`
  - `DB_PASSWORD`: `<AIVEN_PASSWORD>`
  - `VYNK_FLYWAY_ENABLED`: `true`
  - `VYNK_FRONTEND_BASE_URL`: `https://<YOUR_VERCEL_APP>.vercel.app`
  - `VYNK_SESSION_COOKIE_SECURE`: `true`
  - `VYNK_SESSION_COOKIE_SAMESITE`: `none`
  - `VYNK_MAIL_USERNAME`: `<GMAIL_ADDRESS>`
  - `VYNK_MAIL_PASSWORD`: `<GMAIL_APP_PASSWORD>`
- **Healthcheck Endpoint:** `/health` or `/api/health`

### 3. Frontend: Vercel
- Connect this repository to Vercel.
- **Root Directory:** `frontend`
- **Framework Preset:** Vite
- **Build Command:** `npm run build`
- **Output Directory:** `dist`
- **Environment Variables:**
  - `VITE_API_URL`: `https://<YOUR_RAILWAY_APP>.up.railway.app`
- Single-page application routing is managed via `frontend/vercel.json`.

---

## Local Development

### Backend
```bash
cd backend
./mvnw spring-boot:run
```
*(Runs on `http://localhost:8080`. Falls back to in-memory H2 if no external MySQL is configured).*

### Frontend
```bash
cd frontend
npm install
npm run dev
```
*(Runs on `http://localhost:5173` and proxies `/api` requests to backend at port 8080).*
