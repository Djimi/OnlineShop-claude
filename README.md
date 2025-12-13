# OnlineShop-claude
This is test repository to try claude code

## Project Structure

This project consists of microservices for an online shop application.

### Items Microservice

A Spring Boot microservice that manages item inventory with PostgreSQL database.

## Prerequisites

- Java 21
- Maven 3.6+
- Docker and Docker Compose

## Database Setup

### Starting PostgreSQL with Docker Compose

1. Start the PostgreSQL database:
```bash
docker-compose up -d
```

This will:
- Create a PostgreSQL 16 container named `items-postgres`
- Create a database named `items`
- Set up user `postgres` with password `password`
- Expose PostgreSQL on port `5432`
- Initialize the database schema and data from SQL scripts in `init-db/`

2. Verify the database is running:
```bash
docker-compose ps
```

3. To stop the database:
```bash
docker-compose down
```

4. To stop and remove all data (volumes):
```bash
docker-compose down -v
```

### Database Configuration

The database is configured with:
- **Database name**: `items`
- **Username**: `postgres`
- **Password**: `password`
- **Host**: `localhost`
- **Port**: `5432`

Configuration can be found in `Items/src/main/resources/application.yml`

### Database Schema

The `items` table has the following structure:
- `id` (BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY) - Auto-incrementing item ID
- `name` (VARCHAR(255) NOT NULL) - Item name
- `quantity` (INTEGER NOT NULL) - Available quantity
- `description` (VARCHAR(500)) - Item description

## Running the Items Service

1. Make sure PostgreSQL is running (see Database Setup above)

2. Navigate to the Items directory:
```bash
cd Items
```

3. Build and run the service:
```bash
mvn spring-boot:run
```

The service will start on port `9000`.

4. Test the API:
```bash
curl http://localhost:9000/api/v1/items
```

## API Endpoints

### Items Service (Port 9000)

- `GET /api/v1/items` - Get all items

## Development

The Items service uses:
- Spring Boot 3.4.1
- Spring Data JPA with Hibernate
- PostgreSQL Driver
- Lombok for boilerplate reduction
- Virtual threads enabled for better performance
