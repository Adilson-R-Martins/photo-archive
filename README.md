# PhotoArchive - Photo Club Collection Manager

Professional collection management system for photo clubs, focusing on traceability, technical registration, and authorship of photographic works.

## 🚀 Tech Stack
- **Java 17** (LTS)
- **Spring Boot 3.x**
- **Spring Security** (JWT Authentication)
- **Spring Data JPA** / **Hibernate**
- **MySQL**
- **Maven**
- **Lombok**
- **Metadata-extractor** (EXIF processing)

## ✨ Key Features
- **RBAC Security**: Role-Based Access Control using JWT.
- **Professional Metadata**: Automatic extraction of EXIF data (ISO, Aperture, Shutter Speed, Camera Model).
- **High-Res Support**: Optimized for professional photography (up to 50MB per file).
- **Categorization**: Many-to-Many relationship between photos and genres.
- **Traceability**: Photo track record for events, awards, and honors.

## 🛠 Getting Started
### Database Configuration
Create a database named `photoarchive_db` in your MySQL. The application is configured to automatically create tables on startup.

### File Storage
The system automatically creates an `uploads/photos` directory in the project root to store original high-resolution files.

## 📂 API Endpoints

### Authentication
- `POST /api/auth/signin`: User login to receive JWT.
- `POST /api/auth/signup`: User registration.

### Categories
- `GET /api/categories`: List all categories.
- `POST /api/categories`: Create a new category (**Admin only**).

### Photos
- `GET /api/photos`: List all metadata for archived photos.
- `POST /api/photos/upload`: Upload high-res photo + metadata.
    - **Body**: `multipart/form-data`
    - **Params**: `file` (Binary), `title`, `artisticAuthorName`, `categoryIds` (List).

## 🔒 Security Policy
- **Max File Size**: 50MB
- **Allowed MIME Types**: `image/*` (JPEG, PNG, TIFF)
- **Directory Traversal Protection**: Filenames are sanitized and renamed using UUID to prevent attacks and overwriting.