# Restaurant Queue Management System

A customer queue management system for a sports restaurant built with Spring Boot (Java) and a simple HTML/CSS/JavaScript frontend.

## Features

- **Signup Queue**: Holds up to 1000 customers waiting to join the reservation queue
- **Reservation Queue**: Holds up to 1000 customers ready for reservation
- **Employee Queues**: Each employee has their own queue with configurable limits
- **FIFO Ordering**: First-In-First-Out queue management
- **Duplicate Prevention**: Uses HashSet to prevent duplicate customers in queues
- **Auto-Pull**: Employees can manually pull customers from the reservation queue
- **Smart Assignment**: Automatically assigns customers to the employee with the shortest queue
- **Basic Authentication**: Username/password authentication for customers and employees

## Prerequisites

- Java 17 or higher
- Maven 3.6+

## Database Setup

The application uses an embedded H2 database - no installation or configuration required!

The database runs in-memory and is automatically created when the application starts.

**Optional**: For debugging, you can access the H2 console at `http://localhost:8080/h2-console` with:
- JDBC URL: `jdbc:h2:mem:queue_management`
- Username: `sa`
- Password: (leave empty)

## Running the Application

1. Navigate to the project directory:
   ```bash
   cd queue-management-system
   ```

2. Build the project with Maven:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

4. Access the application at: `http://localhost:8080`

## API Endpoints

### Customers
- `POST /api/customers` - Add a new customer to signup queue
- `GET /api/queues/signup` - Get all customers in signup queue
- `GET /api/queues/reservation` - Get all customers in reservation queue

### Employees
- `POST /api/employees` - Add a new employee
- `GET /api/employees` - Get all employees
- `GET /api/queues/employee/{employeeId}` - Get employee's queue
- `POST /api/queues/employee/{employeeId}/pull` - Pull customer from reservation to specific employee queue

### Queue Management
- `POST /api/queues/reservation/{customerId}` - Move customer from signup to reservation queue
- `POST /api/queues/assign-shortest` - Assign customer from reservation to employee with shortest queue
- `GET /api/stats` - Get queue statistics

## Queue Limits

- Signup Queue: 1000 customers
- Reservation Queue: 1000 customers
- Employee Queues: Configurable per employee (set during employee creation)

## Queue Flow

1. Customer signs up → Added to Signup Queue
2. Customer moves to Reservation Queue (manual or automatic)
3. If an employee has capacity, the customer is assigned to an employee queue:
   - **Option A**: Manually pull to a specific employee's queue
   - **Option B**: Automatically assign to employee with shortest queue
4. Moving a customer from the waitlist moves only that selected customer. Available employees automatically receive customers from the main queue in FIFO order; when all employee queues are full, customers remain in the main queue until capacity opens.
5. Employee processes customers in their queue (FIFO)

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Spring Data JPA, Spring Security
- **Database**: H2 (in-memory embedded database)
- **Frontend**: HTML, CSS, JavaScript
- **Build Tool**: Maven

## Project Structure

```
queue-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/restaurant/queue/
│   │   │   ├── config/          # Security configuration
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── model/           # JPA entities
│   │   │   ├── repository/      # JPA repositories
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── static/          # Frontend files
│   │       └── application.properties
└── pom.xml
```

## Notes

- The application uses in-memory HashSet for duplicate detection in signup and reservation queues
- Queue positions are automatically updated when customers move between queues
- The frontend auto-refreshes every 5 seconds to show real-time queue status
