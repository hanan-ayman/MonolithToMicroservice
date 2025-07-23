# Vending Machine API

A RESTful API for a vending machine system built with Spring Boot, featuring JWT authentication, role-based access control, and comprehensive transaction management.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
- [Postman Collection](#postman-collection)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Project Structure](#project-structure)

## Features

- **JWT Authentication & Authorization**
- **Role-based Access Control** (BUYER/SELLER)
- **Multi-item Purchase Support**
- **Coin Deposit System** (5, 10, 20, 50, 100 cents)
- **Change Calculation**
- **Product Management** (CRUD operations)
- **User Management**
- **Comprehensive Error Handling**
- **Input Validation**
- **H2 In-Memory Database** (for development)
- **MySQL Support** (for production)

## Tech Stack

- **Java 17**
- **Spring Boot 3.5.3**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA**
- **H2 Database** (Development)
- **MySQL** (Production)
- **Maven** (Build Tool)
- **Lombok** (Code Generation)
- **Hibernate Validator** (Input Validation)

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL** (for production) or use H2 for development

## Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd vending-machine
```

### 2. Database Configuration

#### For Development (H2 Database)
The application is configured to use H2 in-memory database by default. No additional setup required.

#### For Production (MySQL)
Update `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vending_machine
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
```

### 3. Build the Application
```bash
mvn clean install
```

## Running the Application

### Using Maven
```bash
mvn spring-boot:run
```

### Using Java
```bash
java -jar target/vending-machine-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## Authentication

The API uses JWT (JSON Web Tokens) for authentication. All protected endpoints require a valid JWT token in the Authorization header.

### Getting a JWT Token

1. **Register a new user** (optional):
```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "buyer1",
    "password": "password123",
    "role": "BUYER"
  }'
```

2. **Login to get JWT token**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "buyer1",
    "password": "password123"
  }'
```

3. **Use the token** in subsequent requests:
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/api/v1/products
```

## API Endpoints

### Authentication Endpoints
| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| POST | `/api/v1/auth/login` | User login | Public |
| POST | `/api/v1/auth/signup` | User registration | Public |

### Product Management
| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| GET | `/api/v1/products` | Get all products | Public |
| POST | `/api/v1/products` | Create product | SELLER |
| PUT | `/api/v1/products/{productName}` | Update product | SELLER |
| DELETE | `/api/v1/products/{productName}` | Delete product | SELLER |

### User Management
| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| PUT | `/api/v1/users` | Update user profile | Authenticated |
| DELETE | `/api/v1/users/{userName}` | Delete user | Authenticated |

### Vending Machine Operations
| Method | Endpoint | Description | Access |
|--------|----------|-------------|---------|
| POST | `/api/v1/vendors/deposit` | Deposit coins | BUYER |
| POST | `/api/v1/vendors/buy` | Purchase items | BUYER |
| POST | `/api/v1/vendors/reset` | Reset deposit | BUYER |

## Postman Collection

A Postman collection is available for easy testing and exploration of the API endpoints. You can download the collection from the repository or import it directly into Postman.

### Importing the Collection

1. Open Postman and click on the "Import" button.
2. Select "Link" and enter the URL of the collection.
3. Click "Import" to add the collection to your Postman workspace.

### Using the Collection

1. Open the collection and select an endpoint.
2. Review the request details, including the method, URL, headers, and body.
3. Click the "Send" button to execute the request.
4. Review the response, including the status code, headers, and body.

## Request/Response Examples

### 1. Deposit Coins
```bash
curl -X POST http://localhost:8080/api/v1/vendors/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"amount": 50}'
```

**Response:**
```
"Successfully deposited Successfully deposited 50 cents. New balance: 50 cents"
```

### 2. Purchase Items (Single Item)
```bash
curl -X POST http://localhost:8080/api/v1/vendors/buy \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "items": [
      {
        "productName": "Coca Cola",
        "amountOfProducts": 2
      }
    ]
  }'
```

### 3. Purchase Items (Multiple Items)
```bash
curl -X POST http://localhost:8080/api/v1/vendors/buy \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "items": [
      {
        "productName": "Coca Cola",
        "amountOfProducts": 2
      },
      {
        "productName": "Pepsi",
        "amountOfProducts": 1
      },
      {
        "productName": "Snickers",
        "amountOfProducts": 3
      }
    ]
  }'
```

**Response:**
```
Purchase successful!
Items purchased:
- Coca Cola x2 = 100 cents
- Pepsi x1 = 50 cents
- Snickers x3 = 150 cents
Total spent: 300 cents
Change: 1x100, 1x50
```

### 4. Create Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SELLER_TOKEN" \
  -d '{
    "productName": "Coca Cola",
    "amountAvailable": 10,
    "cost": 50
  }'
```

## Error Handling

The API provides comprehensive error handling with clear, user-friendly messages:

### Common Error Responses

#### 400 Bad Request
```json
{
  "error": "Invalid request body",
  "message": "Request body is required but was not provided"
}
```

#### 401 Unauthorized
```json
{
  "error": "Token expired",
  "message": "Your authentication token has expired. Please log in again."
}
```

#### 403 Forbidden
```json
{
  "error": "Forbidden",
  "message": "You don't have permission to access this resource"
}
```

#### 404 Not Found
```json
{
  "error": "Product not found with name: NonExistentProduct"
}
```

## Testing

### Run Tests
```bash
mvn test
```

### Test Coverage
```bash
mvn jacoco:report
```

## Project Structure

```
src/
├── main/
│   ├── java/com/flapkap/vending_machine/
│   │   ├── config/          # Security & JWT configuration
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Exception handling
│   │   ├── entity/          # JPA entities
│   │   ├── repository/     # Data repositories
│   │   ├── security/       # Security components
│   │   └── service/        # Business logic
│   └── resources/
│       └── application.properties
└── test/                   # Test classes
```

## Validation Rules

### Deposit
- Only coins of 5, 10, 20, 50, or 100 cents are accepted

### Purchase
- Product name must exist
- Sufficient stock must be available
- User must have sufficient deposit

### User Roles
- **BUYER**: Can deposit coins, buy products, reset deposit
- **SELLER**: Can manage products (CRUD operations)

---

**Happy Vending!** 
