# CodeRank Platform - Authentication and IDE Workspace

This repository houses the core components for **CodeRank**, an online code execution platform that allows developers to write, compile, and run code in multiple languages (Java, Python, JavaScript) securely.

This phase of the project implements:
1. **React UI (CodeRank)**: A highly interactive, dark-themed coding IDE workspace, including user registration, login, and a simulated AWS Lambda execution console.
2. **User Service**: A Spring Boot microservice handling account creation, JWT authentication, and user profile lookups.
3. **Code Service**: A Spring Boot microservice that stores code submissions, enforces auth, and orchestrates execution.

---

## 🛠️ Prerequisites

Ensure you have the following installed on your machine:
- **Java Development Kit (JDK) 21** (Corretto 21 or equivalent)
- **Node.js** (v18.x or newer) and **NPM**
- **PostgreSQL** (v15 or newer)
- **Docker Desktop**

---

## 💾 1. Database Setup

CodeRank uses PostgreSQL for user account persistence.

1. Open your PostgreSQL terminal (`psql`) or a database management client (e.g., pgAdmin, DBeaver).
2. Connect with admin privileges and run:
   ```sql
   CREATE DATABASE coderank_users;
  CREATE DATABASE coderank_code;
   ```
3. The local configuration defaults to:
   - **Host**: `localhost:5432`
   - **Database**: `coderank_users`
   - **Username**: `postgres`
   - **Password**: `postgres`

---

## ☕ 2. User Service (Spring Boot Backend)

The service is pre-configured with active environment profiles for local development and production.

### Profiles Configuration
- **Development (`application-dev.yaml`)**:
  Connects to PostgreSQL at `localhost:5432/coderank_users` with hardcoded credentials (`postgres`/`postgres`).
- **Production (`application-prod.yaml`)**:
  Pulls credentials dynamically from the environment. You must set the following environment variables:
  ```powershell
  $env:DB_HOST="production-db-url"
  $env:DB_PORT="5432"
  $env:DB_NAME="coderank_users"
  $env:DB_USER="prod_user"
  $env:DB_PASSWORD="securepassword"
  $env:JWT_SECRET="your-super-secret-hmac-sha256-string-at-least-256-bits"
  ```

### Run the Backend Service
From the root of the project, navigate to the `User Service` folder and execute the Maven Wrapper:

```bash
cd "User Service"
# On Windows PowerShell:
./mvnw.cmd spring-boot:run

# On Linux/macOS:
./mvnw spring-boot:run
```
The server will start listening on port **`8080`**.

---

## ⚙️ 3. Code Service (Spring Boot Backend)

The service stores code submissions and invokes AWS Lambda functions for running code. It reuses the same JWT secret as the User Service for authentication.

### Profiles Configuration
- **Development (`application-dev.yaml`)**:
  Connects to PostgreSQL at `localhost:5432/coderank_code` with hardcoded credentials (`postgres`/`postgres`).
- **Production (`application-prod.yaml`)**:
  Pulls credentials dynamically from the environment. You must set the following environment variables:
  ```powershell
  $env:DB_HOST="production-db-url"
  $env:DB_PORT="5432"
  $env:DB_NAME="coderank_code"
  $env:DB_USER="prod_user"
  $env:DB_PASSWORD="securepassword"
  $env:JWT_SECRET="your-super-secret-hmac-sha256-string-at-least-256-bits"
  $env:EXECUTION_MODE="aws"
  $env:EXECUTION_AWS_REGION="ap-south-1"
  $env:EXECUTION_AWS_PROFILE="coderank"
  $env:EXECUTION_LAMBDA_JS="coderank-exec-js"
  $env:EXECUTION_LAMBDA_PYTHON="coderank-exec-python"
  $env:EXECUTION_LAMBDA_JAVA="coderank-exec-java"
  ```

### Run the Code Service
From the root of the project, navigate to the `Code Service` folder and execute the Maven Wrapper:

```bash
cd "Code Service"
# On Windows PowerShell:
./mvnw.cmd spring-boot:run

# On Linux/macOS:
./mvnw spring-boot:run
```
The server will start listening on port **`8081`**.

### Execution (Lambda) Setup
Docker Engine should be running
Code Service can invoke three AWS Lambda functions (Java, Python, JavaScript) for code execution. For local validation and AWS setup, follow the guide in:
`Code Service/README_EXECUTION_LAMBDA_SETUP.md`

Quick SAM local steps (run from a terminal):
```powershell
cd "Execution-Lambda"
sam build --use-container
sam local start-lambda --host 127.0.0.1 --port 3001
```

For full setup, troubleshooting, and test-invoke commands, refer to `Code Service/README_EXECUTION_LAMBDA_SETUP.md`.

Required environment variables for local SAM execution (example):
```powershell
$env:EXECUTION_MODE="sam-local"
$env:EXECUTION_SAM_ENDPOINT="http://127.0.0.1:3001"
$env:EXECUTION_AWS_REGION="ap-south-1"
$env:EXECUTION_AWS_PROFILE="coderank"
$env:EXECUTION_LAMBDA_JS="coderank-exec-js"
$env:EXECUTION_LAMBDA_PYTHON="coderank-exec-python"
$env:EXECUTION_LAMBDA_JAVA="coderank-exec-java"
```

---

## ⚛️ 4. React UI (CodeRank Frontend)

Built with **React 19**, **TypeScript**, **Vite**, and **Material UI**.

### Run the Frontend
1. Navigate to the `CodeRank` folder from the root of the project:
   ```bash
   cd CodeRank
   ```
2. Install npm dependencies (if not already installed):
   ```bash
   npm install --legacy-peer-deps
   ```
3. Run the Vite development server:
   ```bash
   npm run dev
   ```
4. Open the displayed URL (usually `http://localhost:5173`) in your browser.

---

## 📡 5. API Endpoint Schema

All backend auth endpoints are prefixed with `/api`.

### Authentication Endpoints

#### 1. Sign Up (Local Registration)
- **Method**: `POST`
- **Path**: `/api/auth/signup`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "name": "Alex Mercer",
    "email": "alex@coderank.com",
    "password": "securepassword123"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "name": "Alex Mercer",
    "email": "alex@coderank.com",
    "roles": ["ROLE_USER"]
  }
  ```

#### 2. Sign In (Local Login)
- **Method**: `POST`
- **Path**: `/api/auth/login`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "email": "alex@coderank.com",
    "password": "securepassword123"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "name": "Alex Mercer",
    "email": "alex@coderank.com",
    "roles": ["ROLE_USER"]
  }
  ```

#### 3. JWT Token Validation (Internal Microservice Check)
- **Method**: `GET`
- **Path**: `/api/auth/validate`
- **Query Params**: `token=eyJhbGciOiJIUzI1NiJ9...`
- **Response (200 OK)**:
  ```json
  {
    "email": "alex@coderank.com",
    "roles": ["ROLE_USER"]
  }
  ```

### User Resource Endpoints (Protected)

#### 1. Fetch Current User Profile
- **Method**: `GET`
- **Path**: `/api/users/me`
- **Headers**: `Authorization: Bearer <JWT_TOKEN>`
- **Response (200 OK)**:
  ```json
  {
    "id": 1,
    "name": "Alex Mercer",
    "email": "alex@coderank.com",
    "roles": ["ROLE_USER"]
  }
  ```

### Code Submission Endpoints (Protected)

#### 1. Submit Code (Persist + Execute)
- **Method**: `POST`
- **Path**: `/api/code/submit`
- **Headers**: `Authorization: Bearer <JWT_TOKEN>`
- **Request Body**:
  ```json
  {
    "code": "public class Main { public static void main(String[] args) { System.out.println(\"Hi\"); } }",
    "language": "java",
    "tagName": "hello-world"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "id": 101,
    "userEmail": "alex@coderank.com",
    "code": "public class Main...",
    "language": "java",
    "tagName": "hello-world",
    "status": "COMPLETED",
    "output": "Hi\n",
    "executionTimeMs": 45,
    "memoryUsedMb": 64,
    "submittedAt": "2026-05-29T11:40:12.123"
  }
  ```

#### 2. Save Code (Persist Only)
- **Method**: `POST`
- **Path**: `/api/code/save`
- **Headers**: `Authorization: Bearer <JWT_TOKEN>`
- **Request Body**:
  ```json
  {
    "code": "print('draft')",
    "language": "python",
    "tagName": "draft"
  }
  ```

#### 3. List Submissions
- **Method**: `GET`
- **Path**: `/api/code/submissions`
- **Headers**: `Authorization: Bearer <JWT_TOKEN>`
- **Query Params (Optional)**:
  - `language=java`
  - `taggedOnly=true`
