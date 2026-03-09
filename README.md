# KodeX Backend

Backend service for **KodeX**, a browser-based cloud IDE that allows users to create, manage, and run development projects entirely from the browser.

This backend is responsible for authentication, project management, container orchestration, file synchronization with Google Drive, and real-time communication with the frontend.

---

## Overview

The backend powers the core functionality of the KodeX platform by providing:

- REST APIs for project and user management
- Secure authentication using JWT
- Containerized development environments
- File synchronization with Google Drive
- WebSocket services for real-time communication

Each authenticated user interacts with an isolated environment where their projects can be edited, executed, and persisted.

---

## Tech Stack

- **Java**
- **Spring Boot**
- **Spring Security**
- **JWT Authentication**
- **PostgreSQL**
- **Docker**
- **WebSockets**
- **Google Drive API**

---

## Key Features

### REST APIs

The backend exposes a set of RESTful APIs used by the frontend to interact with the system.

These APIs handle operations such as:

- Authentication
- Token refreshing
- Project management
- File operations
- Container lifecycle management

The API layer ensures clear separation between the frontend client and backend services.

---

### CRUD Operations

The backend provides full **CRUD (Create, Read, Update, Delete)** support for managing user projects.

Typical operations include:

- Creating new projects
- Retrieving project metadata
- Updating project information
- Deleting existing projects

This allows users to manage their workspaces directly from the web interface.

---

### JWT Authentication

Authentication is implemented using **JSON Web Tokens (JWT)**.

The authentication flow works as follows:

1. User logs in through the authentication endpoint.
2. JWT access and refresh tokens are issued.
3. The frontend includes the access token in subsequent API requests.
4. Spring Security validates the token before allowing access to protected resources.

This provides stateless authentication while maintaining secure access control.

---

### File Synchronization Engine

KodeX integrates with **Google Drive** to persist user projects.

A file synchronization engine handles:

- Uploading project files to the user's Google Drive
- Downloading files when projects are opened
- Maintaining consistency between the cloud IDE and the user's storage

This ensures that user data remains persistent even if backend containers are restarted.

---

### Docker-Based Development Environments

Each user is provided with an isolated **Docker container** that acts as their development environment.

Containers provide:

- Secure process isolation
- Access to a Linux shell
- Execution of user code

The backend manages container lifecycle operations including:

- Container creation
- Shell session management
- Container cleanup

---

### WebSocket Communication

WebSockets are used for **low-latency, real-time communication** between the backend and the frontend.

Primary use cases include:

- Streaming terminal input and output
- Real-time shell interaction
- Live data transfer between the user's browser and their container

This allows the browser-based terminal to behave similarly to a local terminal session.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven
- Docker
- Google Cloud credentials for Drive API
- IntelliJ IDEA(for IDE)

---

### Installation

1. clone the repository
2. open the project's root folder in your preferred IDE
3. open a terminal
4. ```mvn clean install```
 
### Running the server

```mvn spring-boot:run```

## Related Repository

### Frontend repository:
[KodeX Frontend](https://github.com/Subhashish005/KodeX-Frontend)

The frontend provides the user interface for interacting with this backend service.