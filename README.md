# Cloud Architecture Designer API

This is the backend API for the Cloud Architecture Designer application, a tool for designing and visualizing AWS cloud architectures.

## Overview

The Cloud Architecture Designer API provides the server-side functionality for the application, including:

- User authentication and management
- AWS cost estimation
- Terraform code generation
- Data persistence

## Technology Stack

- Java 17
- Spring Boot
- Spring Security with JWT authentication
- JPA/Hibernate for database access
- H2 Database (development)
- PostgreSQL (production)
- Maven for dependency management

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL (for production deployment)

### Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/cloud-architecture-designer-api.git
cd cloud-architecture-designer-api
```

2. Configure the application
   - Update `application.properties` with your database and mail server settings
   - For development, the default H2 database configuration can be used

3. Build the application
```bash
mvn clean install
```

4. Run the application
```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8081`

## API Documentation

### Authentication Endpoints

- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - User login
- `GET /api/auth/verify` - Email verification
- `POST /api/auth/resend-verification` - Resend verification email
- `POST /api/auth/refresh-token` - Refresh JWT token
- `POST /api/auth/logout` - User logout

### Admin Endpoints

- `GET /api/admin/users` - Get all users (Admin only)
- `POST /api/admin/users/{userId}/verify` - Manually verify user (Admin only)
- `POST /api/admin/users/{userId}/admin` - Make user an admin (Admin only)

### Cost Calculation Endpoints

- `GET /api/cost` - Get cost estimate for current architecture
- `POST /api/cost` - Update components for cost calculation

### Health Check

- `GET /api/health` - API health check

## Security

The API implements several security measures:

- JWT-based authentication
- Password encryption using BCrypt
- Role-based access control
- Rate limiting for login and registration attempts
- Email verification for new accounts

## Development

### Project Structure

- `src/main/java/com/stagllc/staginfra` - Main source code
  - `config` - Configuration classes
  - `controller` - REST controllers
  - `dto` - Data Transfer Objects
  - `model` - Entity models
  - `repository` - Data repositories
  - `security` - Security configuration
  - `service` - Business logic services

### Testing

Run the tests with:

```bash
mvn test
```

## Deployment

The application can be packaged as a JAR file:

```bash
mvn package
```

The resulting JAR file can be found in the `target` directory and can be run with:

```bash
java -jar target/staginfra-0.0.1-SNAPSHOT.jar
```

## License

[Your License Here]