# AGENTS.md - StingrayTV Alice Project Guide

## Project Structure and Module Organization

This Spring Boot project integrates Yandex's "Home with Alice" smart home API with TriColor satellite receivers. The
project structure follows standard Maven layout:

- `src/main/java/` - Main application source code
    - `ru.oldzoomer.stingraytv_alice` - Root package containing main application classes
    - `controller/` - REST controllers for Yandex Home API endpoints
    - `service/` - Business logic services
    - `gateway/` - Communication layer with TriColor receivers
    - `dto/` - Data Transfer Objects
    - `config/` - Spring configuration classes including security and properties
    - `enums/` - Enumerations used throughout the application

- `src/test/java/` - Unit and integration tests
- `src/main/resources/` - Configuration files, properties
- `Dockerfile` and `docker-compose.yml` - Containerization configuration

## Core Components

1. **Controllers**:
    - `YandexSmartHomeController.java` - Main controller handling all Yandex Smart Home API endpoints including device
      discovery, state queries, and actions

2. **Services**:
    - `YandexSmartHomeService.java` - Business logic for processing Yandex requests
    - `StingrayTVService.java` - Service for communicating with TriColor receivers
    - `StingrayDeviceDiscoveryService.java` - Device discovery functionality

3. **Gateway Layer**:
    - `YandexSmartHomeGateway.java` - Main gateway coordinating between Yandex API and TriColor receiver

4. **Security Configuration**:
    - `SecurityConfig.java` - Spring Security configuration using Keycloak for authentication
    - `KeycloakConverter.java` - Custom converter for Keycloak JWT tokens

## Key Features

- **Authentication**: Uses Keycloak with OAuth2 Resource Server for secure client authentication
- **API Integration**: Implements Yandex Smart Home API endpoints:
    - `GET /v1.0/user/devices` - Device discovery
    - `POST /v1.0/user/devices/query` - State queries
    - `POST /v1.0/user/devices/action` - Device actions
    - `POST /v1.0/user/unlink` - Account unlinking
- **Configuration**: Uses Spring Boot configuration properties with environment variable support
- **Device Communication**: Supports communication with TriColor receivers via configurable IP and port

## Build, Test and Development Commands

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Local Development

```bash
./gradlew bootRun
```

### Docker Deployment

```bash
docker-compose up
```

## Code Style and Naming Conventions

- Java code follows standard Spring Boot conventions
- Package names use reverse domain notation (`ru.oldzoomer`)
- Class names use PascalCase
- Method and variable names use camelCase
- All API endpoints follow REST conventions
- Test classes end with `Test` suffix

## VCS Recommendations: Commits and Pull Requests

### Commit Messages

Follow conventional commit format:

- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation changes
- `style:` for code style changes
- `refactor:` for code restructuring

### Pull Request Requirements

- All tests must pass
- Code coverage should be maintained or improved
- PR descriptions should clearly explain the changes
- Include any relevant issue numbers
