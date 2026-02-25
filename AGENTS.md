# Agent Guidelines for CMS Project

This document provides guidelines for AI coding agents working on this Spring Boot CMS application.

## Project Overview

- **Framework**: Spring Boot 3.5.10
- **Language**: Java 17
- **Build Tool**: Apache Maven 3.9.12 (use Maven Wrapper: `./mvnw`)
- **Database**: MySQL (card_management_system)
- **Data Access**: Spring Data JDBC with Native SQL - **NO JPA/Hibernate allowed**
- **Package Structure**: `com.epic.cms`
- **Server Port**: 8090

## ⚠️ CRITICAL REQUIREMENTS

### NO JPA/Hibernate
- **STRICTLY FORBIDDEN**: Do NOT use JPA, Hibernate, or any ORM framework
- **REQUIRED**: Use Spring Data JDBC with native SQL queries only
- **REASON**: Assignment requirement - must demonstrate SQL knowledge
- All queries MUST be written in native SQL using `@Query` annotation
- Do NOT use JPQL, HQL, or query methods like `findByName()`

### Database Schema
- Main tables: `Card`, `User`, `CardRequest`, `CardStatus`, `RequestStatus`, `CardRequestType`
- Always use correct table names:
  - ✅ `CardStatus` (NOT `Status`)
  - ✅ Column: `s.StatusCode` (NOT `s.Code`)
  - ✅ Column: `u.Name` (NOT `u.FullName`)
- Card numbers are ENCRYPTED in database (VARCHAR(255))
- When writing queries with JOINs, verify table and column names in `src/main/resources/schema.sql`

## Build, Test, and Run Commands

### Building the Project
```bash
# Clean and build
./mvnw clean install

# Build without running tests
./mvnw clean install -DskipTests

# Compile only
./mvnw compile

# Package the application
./mvnw package
```

### Running Tests
```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=CmsApplicationTests

# Run a specific test method
./mvnw test -Dtest=CmsApplicationTests#contextLoads

# Run tests with coverage (add jacoco plugin if needed)
./mvnw verify
```

### Running the Application
```bash
# Run the Spring Boot application
./mvnw spring-boot:run

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run the packaged JAR
java -jar target/cms-0.0.1-SNAPSHOT.jar
```

### Cleaning
```bash
# Clean build artifacts
./mvnw clean
```

## Code Style Guidelines

### Package Structure
```
com.epic.cms/
├── controller/     # REST controllers (@RestController, @Controller)
├── service/        # Business logic (@Service)
├── repository/     # Data access (@Repository, uses Spring Data JDBC)
├── model/          # Domain entities (@Table, @Id, @Column)
├── dto/            # Data Transfer Objects (request/response models)
├── exception/      # Custom exceptions and error handlers
├── config/         # Configuration classes (@Configuration)
└── util/           # Utility classes
```

### Imports
- Use specific imports, avoid wildcard imports (`import java.util.*`)
- Group imports in order: Java standard library, Spring framework, third-party, local packages
- Remove unused imports
- Example:
  ```java
  import java.util.List;
  import java.util.Optional;
  
  import org.springframework.stereotype.Service;
  import org.springframework.beans.factory.annotation.Autowired;
  
  import com.epic.cms.model.User;
  import com.epic.cms.repository.UserRepository;
  ```

### Formatting
- **Indentation**: Use tabs (as configured in the IDE)
- **Line Length**: Keep lines under 120 characters
- **Braces**: Opening brace on same line, closing brace on new line
  ```java
  public void method() {
      // code here
  }
  ```
- **Spacing**: Space after keywords (`if (condition)` not `if(condition)`)
- **Blank Lines**: One blank line between methods, two between classes

### Naming Conventions
- **Classes**: PascalCase (e.g., `UserService`, `OrderController`)
- **Interfaces**: PascalCase, avoid "I" prefix (e.g., `UserRepository` not `IUserRepository`)
- **Methods**: camelCase, use verbs (e.g., `findUserById`, `createOrder`)
- **Variables**: camelCase (e.g., `userName`, `orderList`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE`)
- **Packages**: lowercase, no underscores (e.g., `com.epic.cms.service`)

### Type Usage
- **Prefer interfaces over implementations** for dependencies:
  ```java
  // Good
  private List<User> users;
  
  // Avoid
  private ArrayList<User> users;
  ```
- **Use generics** to avoid unchecked warnings
- **Avoid raw types**: Use `List<String>` not `List`
- **Use Optional** for return values that may be null:
  ```java
  public Optional<User> findById(Long id) {
      // implementation
  }
  ```

### Annotations
- Use Lombok annotations to reduce boilerplate:
  - `@Data` for POJOs (generates getters, setters, toString, equals, hashCode)
  - `@Builder` for builder pattern
  - `@Slf4j` for logging
  - `@RequiredArgsConstructor` for constructor injection
- Spring annotations:
  - `@RestController` for REST endpoints
  - `@Service` for business logic
  - `@Repository` for data access
  - `@Configuration` for configuration classes
  - `@Autowired` for dependency injection (prefer constructor injection)

### Dependency Injection
- **Prefer constructor injection** over field injection:
  ```java
  // Good - constructor injection
  @Service
  @RequiredArgsConstructor
  public class UserService {
      private final UserRepository userRepository;
  }
  
  // Avoid - field injection
  @Service
  public class UserService {
      @Autowired
      private UserRepository userRepository;
  }
  ```

### Error Handling
- **Never swallow exceptions** without logging
- **Use specific exceptions** rather than generic Exception
- **Create custom exceptions** for business logic errors
- **Use @ControllerAdvice** for global exception handling
  ```java
  @ControllerAdvice
  public class GlobalExceptionHandler {
      @ExceptionHandler(ResourceNotFoundException.class)
      public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
          // handle exception
      }
  }
  ```
- **Log at appropriate levels**: ERROR for errors, WARN for warnings, INFO for important events, DEBUG for debugging

### Comments and Documentation
- **Write self-documenting code** with clear names
- **Use Javadoc** for public APIs:
  ```java
  /**
   * Retrieves a user by their unique identifier.
   *
   * @param id the user ID
   * @return an Optional containing the user if found
   */
  public Optional<User> findById(Long id) {
      // implementation
  }
  ```
- **Avoid obvious comments** - comment "why" not "what"
- **Update comments** when code changes

### Testing
- **Test class naming**: Append `Test` or `Tests` to the class name (e.g., `UserServiceTest`)
- **Test method naming**: Use descriptive names that explain the scenario:
  ```java
  @Test
  void findById_whenUserExists_shouldReturnUser() {
      // test implementation
  }
  ```
- **Use JUnit 5** annotations: `@Test`, `@BeforeEach`, `@AfterEach`
- **Use assertions**: Prefer AssertJ or JUnit assertions
- **Mock dependencies** using Mockito: `@Mock`, `@InjectMocks`
- **Use @SpringBootTest** for integration tests

## Configuration

### Application Configuration
- Configuration files are in `src/main/resources/`
- Use `application.yaml` for configuration (YAML preferred over properties)
- Use profiles for environment-specific config: `application-dev.yaml`, `application-prod.yaml`

### Database
- Database connection configured in `application.yaml`
- **Use Spring Data JDBC for database access** (NOT JPA)
- Place SQL scripts in `src/main/resources/` (e.g., `schema.sql`, `data.sql`)
- All queries must be native SQL using `@Query` annotation

## Important Notes

- **This project uses Maven Wrapper** (`./mvnw`) - always use it instead of global Maven
- **Lombok is configured** - ensure annotation processing is enabled in your IDE
- **Java 17 is required** - use modern Java features (records, switch expressions, etc.)
- **Follow Spring Boot conventions** for auto-configuration
- **No linting tools configured yet** - maintain consistent style manually

