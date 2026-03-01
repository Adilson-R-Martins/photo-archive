# PhotoArchive - Photo Club Collection Manager

Professional collection management system for photo clubs, focusing on traceability, technical registration, and authorship of photographic works.

## Tech Stack
- **Java 17** (LTS)
- **Spring Boot 3.x**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA**
- **MySQL**
- **Maven**
- **Lombok**

## Getting Started
### Prerequisites
- JDK 17
- Maven 3.x
- MySQL (via XAMPP or local installation)

### Database Configuration
Create a database named `photoarchive_db` in your MySQL. The application is configured to automatically create tables on startup via `hibernate.ddl-auto=update`.

## Architecture
This project follows a RESTful API architecture with a layered approach:
- **Controller**: Handles HTTP requests.
- **Service**: Contains business logic.
- **Repository**: Manages database communication.
- **Model/Entity**: Represents database tables.