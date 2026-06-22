<p align="center">
  <img src="./logo.png" alt="B.O.B.D.S. Logo" width="150" />
</p>

<h1 align="center">B.O.B.D.S. Server</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Maven" />
  <img src="https://img.shields.io/badge/JSON-000000?style=for-the-badge&logo=json&logoColor=white" alt="JSON" />
</p>

## About The Project

B.O.B.D.S. Server is the robust backend application powering the Base Operativa de Batalla y Defensa del Sistema (B.O.B.D.S.). Built with Spring Boot, it provides a secure and highly responsive REST API designed to handle operator authentication, unit registration, and system logs. It utilizes a lightweight, file-based JSON storage system for rapid local deployment and data persistence.

## Key Features

* **RESTful API Architecture:** Clean, stateless API endpoints for seamless frontend integration.
* **Server-Sent Events (SSE):** Real-time unidirectional streaming endpoints (`/api/stream`) for live updates on orders and unit statuses without polling.
* **Role-Based Access Control (RBAC):** Hierarchical permissions (Propietario, Co-Propietario, Administrador, Operador, Invitado) regulating UI visibility and administrative actions.
* **System Audit Logging:** Comprehensive Action-Log interceptor tracking access, creation, modifications, and command execution across the platform.
* **Unit Linking via Secret Tokens:** Secure association of units to users utilizing expendable or persistent secret codes.
* **Multi-Client Concurrency:** High-performance file access using `ReentrantReadWriteLock` and `Semaphore` to isolate writes without bottlenecking simultaneous reads and ensuring atomic operations.
* **File-based JSON Database:** Custom, lightweight JSON storage engines for users, units, and system logs.
* **Operator Authentication:** Secure login and registration systems with token validation and SHA-256 password hashing.
* **Chronological Order Traceability:** Orders sorted dynamically by timestamps with strict monotonic ID fallbacks.
* **External Integration:** Resilient Java `HttpClient` to transmit orders directly to external robotic simulators with timeout protections.
* **Secure Environment:** Sensitive credentials and local databases are strictly ignored and protected via `.gitignore` and `application-secret.properties`.

## Prerequisites

Before running the application, ensure you have the following installed:
* Java Development Kit (JDK 17 or higher recommended)
* Maven (or use the provided `mvnw` wrapper)

## Installation & Configuration

1. Clone the repository.
2. Navigate to the `server` directory.
3. Configure your secrets: 
   Create a file named `application-secret.properties` inside `server/src/main/resources/` and add your email SMTP password:
   ```properties
   spring.mail.password=YOUR_APP_PASSWORD
   ```

   > [!CAUTION]
   > Never commit `application-secret.properties` to version control. Ensure it is listed in your `.gitignore` to prevent leaking sensitive credentials.
4. Build the project using Maven:
   ```bash
   ./mvnw clean install
   ```
5. Start the Spring Boot server:
   ```bash
   ./mvnw spring-boot:run
   ```

## Usage

> [!NOTE]
> Once the server is running, it will listen on `http://localhost:8080` (or `8081` depending on your `application.properties`).

* **API Endpoints:** Access the API via `/api/auth/*` and `/api/units/*`.
* **Data Storage:** All persistent data will be automatically managed and stored as JSON files inside the `data/` directory at the root of the repository.

> [!IMPORTANT]
> The JSON files act as the primary database. Deleting them will wipe all users, units, and order histories permanently.

## Code Structure

* `server/src/main/java/com/bobds/server/`: Core application logic.
  * `*Controller.java`: REST API endpoints handling routing for Orders, Units, and Authentication.
  * `*Service.java`: Core business logic, concurrency handling, and file-based data management.
  * `*DTO.java`: Data Transfer Objects providing strictly typed payload definitions for client-server communication.
  * `JwtUtil.java` & `JwtFilter.java`: JWT generation and HTTP-only cookie-based authentication middleware.
  * `RobotClient.java`: HTTP client responsible for transmitting orders to external robotic simulators.
* `server/src/main/resources/`: Environment configuration (`application.properties`, `application-secret.properties`).
* `data/`: Local persistent JSON file storage for rapid iteration and database simulation.
## Deployment (Docker & .env)
This component is fully containerized using Docker. All sensitive configuration (such as SMTP credentials and ports) is managed via a .env file located at server/.env (which is excluded via .gitignore to prevent secret leaks).
