# EcoGrid — Peer-to-Peer (P2P) Renewable Energy Trading Platform

EcoGrid is a comprehensive microservices-based peer-to-peer (P2P) renewable energy trading platform. It empowers prosumers (residential solar producers) to sell surplus green energy directly to neighboring consumers via transparent matching and settlement mechanisms.

---

## 🏗 System Architecture

The platform is designed around Service-Oriented Architecture (SOA) and Spring Cloud Microservices:

| Microservice | Port | Description |
|---|---|---|
| **Eureka Server** | `8761` | Service Registry & Discovery |
| **API Gateway** | `8080` | Unified API routing, CORS handling & gateway |
| **Auth Service** | `8081` | Authentication, JWT issue & validation, role management (CONSUMER, PROSUMER, ADMIN) |
| **Prosumer Service** | `8082` | Prosumer profiles, solar generation capacity & asset specs |
| **Telemetry Service** | `8083` | Smart meter simulation, real-time energy generation/consumption telemetry |
| **Matching Service** | `8084` | Marketplace order book, peer-to-peer trade matching engine |
| **Settlement Service** | `8085` | Automated billing, transaction clearing & trade settlement |
| **Frontend** | `5173` | React 18 + Vite, Tailwind/Modern CSS, Recharts, Lucide Icons |

---

## ⚡ Tech Stack

- **Backend**: Java 21, Spring Boot 3.x, Spring Cloud (Netflix Eureka, Spring Cloud Gateway), Spring Security, JJWT, Spring Data JPA, H2 In-Memory Database
- **Frontend**: React 18, Vite, React Router v6, Axios, Recharts, Lucide React
- **Build Tools**: Apache Maven 3.9+, Node.js 20+

---

## 🚀 Getting Started

### 1. Prerequisites
- Java JDK 21+
- Apache Maven 3.8+
- Node.js 18+ and npm

### 2. Running Microservices
Start each service in the following order:
1. `ecogrid-eureka-server` (Port 8761)
2. `ecogrid-auth-service` (Port 8081)
3. `ecogrid-prosumer-service` (Port 8082)
4. `ecogrid-telemetry-service` (Port 8083)
5. `ecogrid-matching-service` (Port 8084)
6. `ecogrid-settlement-service` (Port 8085)
7. `ecogrid-api-gateway` (Port 8080)

Command for each service:
```bash
cd ecogrid-<service-name>
mvn spring-boot:run
```

### 3. Running the Frontend
```bash
cd frontend
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 👥 User Roles & Access
- **Consumer (USER)**: View marketplace listings, purchase renewable energy, track consumption.
- **Prosumer**: Manage solar assets, log surplus energy, create listings, monitor telemetry.
- **Administrator**: Oversee platform transactions, monitor registered prosumers, view settlement reports.
