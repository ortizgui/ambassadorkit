# Ambassador Kit

## Overview
Ambassador Kit is a microservice-based project that implements the Ambassador pattern, serving as an API gateway for product-related operations. This project is built using Kotlin and Spring Boot, leveraging modern microservice architecture patterns.

## Architecture
The project follows a microservice architecture where:
- It acts as an ambassador/gateway for product-related operations
- Communicates with external services using Feign Client
- Implements RESTful API endpoints for product management

## Features
- Product management operations:
  - Get product by ID
  - List products with pagination
  - Create new products
  - Update existing products
  - Delete products
  - Filter products by category

## Technical Stack
- Kotlin
- Spring Boot
- Spring Cloud OpenFeign
- Gradle (with Kotlin DSL)
- Docker support

## Project Structure
```
ambassador/
├── src/
│   └── main/
│       └── kotlin/
│           └── infrastructure/
│               └── client/
│                   └── product/
├── wiremock/
├── build.gradle.kts
├── docker-compose.yml
└── Dockerfile
```

## Getting Started

### Prerequisites
- JDK 17 or higher
- Docker and Docker Compose
- Gradle

### Running the Application
1. Clone the repository
2. Build the project:
   ```bash
   ./gradlew build
   ```
3. Run using Docker Compose:
   ```bash
   docker-compose up
   ```

## API Endpoints
The service provides the following endpoints:
- `GET /products/{id}` - Get product by ID
- `GET /products` - List products with pagination
- `POST /products` - Create a new product
- `PUT /products/{id}` - Update an existing product
- `DELETE /products/{id}` - Delete a product
- `GET /products/category/{category}` - Get products by category

## Configuration
The service can be configured through:
- Environment variables
- Application properties
- Docker Compose configuration

## Development
The project uses Gradle with Kotlin DSL for build management. Key build files:
- `build.gradle.kts` - Main build configuration
- `settings.gradle.kts` - Project settings
- `gradle.properties` - Gradle properties