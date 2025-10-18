# 🤝 Contributing to ExploreSG Fleet Service

Thank you for your interest in contributing to the ExploreSG Fleet Service! This document provides guidelines and instructions for contributing to this project.

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Documentation](#documentation)
- [Issue Reporting](#issue-reporting)

---

## 📜 Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors. We expect:

- ✅ Respectful and constructive communication
- ✅ Collaborative problem-solving
- ✅ Focus on what's best for the project
- ❌ No harassment, discrimination, or unprofessional behavior

---

## 🚀 Getting Started

### Prerequisites

Before you start contributing, ensure you have:

- **Java 17** or higher
- **Maven 3.8+**
- **PostgreSQL 15** (or Docker)
- **Git**
- **Your favorite IDE** (IntelliJ IDEA, Eclipse, VS Code)

### Fork and Clone

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:

   ```bash
   git clone https://github.com/YOUR_USERNAME/exploresg-fleet-service.git
   cd exploresg-fleet-service
   ```

3. **Add upstream remote:**

   ```bash
   git remote add upstream https://github.com/XploreSG/exploresg-fleet-service.git
   ```

4. **Create your `.env` file** (see [Environment Setup](../README.md#environment-setup))

5. **Run the application** to verify setup:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🔄 Development Workflow

### 1. Create a Feature Branch

Always create a new branch for your work:

```bash
# Update your local main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/EXPLORE-123-add-new-feature
```

**Branch Naming Convention:**

```
feature/EXPLORE-XXX-short-description    # New features
bugfix/EXPLORE-XXX-short-description     # Bug fixes
hotfix/EXPLORE-XXX-critical-fix          # Critical production fixes
docs/update-readme                       # Documentation changes
refactor/improve-service-layer           # Code refactoring
test/add-integration-tests               # Test additions
```

### 2. Make Your Changes

- Write clean, readable code
- Follow the [Coding Standards](#coding-standards)
- Add tests for new functionality
- Update documentation as needed

### 3. Commit Your Changes

Follow the [Commit Message Guidelines](#commit-message-guidelines):

```bash
git add .
git commit -m "feat: add vehicle availability endpoint"
```

### 4. Keep Your Branch Updated

Regularly sync with upstream:

```bash
git fetch upstream
git rebase upstream/main
```

### 5. Push and Create Pull Request

```bash
git push origin feature/EXPLORE-123-add-new-feature
```

Then create a Pull Request on GitHub.

---

## 💻 Coding Standards

### Java Code Style

We follow **Google Java Style Guide** with some modifications.

#### Class Structure

```java
package com.exploresg.fleetservice.service;

// Imports organized by: java.*, javax.*, third-party, local
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.exploresg.fleetservice.model.CarModel;
import com.exploresg.fleetservice.repository.CarModelRepository;

/**
 * Service class for managing car models.
 * Handles business logic for car model operations.
 */
@Service
@RequiredArgsConstructor
public class CarModelService {

    private final CarModelRepository carModelRepository;

    /**
     * Retrieves all car models.
     *
     * @return list of all car models
     */
    public List<CarModel> getAllCarModels() {
        return carModelRepository.findAll();
    }
}
```

#### Naming Conventions

| Element    | Convention       | Example                      |
| ---------- | ---------------- | ---------------------------- |
| Classes    | PascalCase       | `CarModelService`            |
| Interfaces | PascalCase       | `ReservationRepository`      |
| Methods    | camelCase        | `createReservation()`        |
| Variables  | camelCase        | `carModel`, `userId`         |
| Constants  | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`            |
| Packages   | lowercase        | `com.exploresg.fleetservice` |

#### Code Formatting

- **Indentation:** 4 spaces (no tabs)
- **Line Length:** 120 characters max
- **Braces:** Always use braces, even for single-line statements
- **Blank Lines:** Use blank lines to separate logical sections

```java
// Good ✅
if (condition) {
    doSomething();
}

// Bad ❌
if (condition)
    doSomething();
```

### Spring Boot Best Practices

#### 1. Use Constructor Injection

```java
// Good ✅
@Service
@RequiredArgsConstructor
public class FleetService {
    private final FleetRepository fleetRepository;
}

// Bad ❌
@Service
public class FleetService {
    @Autowired
    private FleetRepository fleetRepository;
}
```

#### 2. Use DTOs for API Responses

```java
// Good ✅
@GetMapping("/models")
public ResponseEntity<List<CarModelDto>> getModels() {
    return ResponseEntity.ok(carModelService.getAllModels());
}

// Bad ❌
@GetMapping("/models")
public ResponseEntity<List<CarModel>> getModels() {
    return ResponseEntity.ok(carModelRepository.findAll());
}
```

#### 3. Handle Exceptions Properly

```java
@Service
public class ReservationService {

    public Reservation createReservation(CreateReservationRequest request) {
        // Validate input
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new InvalidDateRangeException("Start date must be before end date");
        }

        // Business logic
        return reservationRepository.save(reservation);
    }
}
```

#### 4. Use Lombok Wisely

```java
// Good ✅
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarModelDto {
    private UUID id;
    private String modelName;
    private String manufacturer;
}

// Avoid @Data on entities (use @Getter/@Setter instead)
@Entity
@Getter
@Setter
@NoArgsConstructor
public class CarModel {
    // ...
}
```

---

## 🧪 Testing Guidelines

### Test Coverage Requirements

- **Minimum Coverage:** 80%
- **Target Coverage:** 85%+
- **Critical Paths:** 100% coverage

### Test Structure

```
src/test/java/
├── controller/
│   ├── FleetControllerUnitTest.java       # Unit tests with mocks
│   └── FleetControllerIntegrationTest.java # Integration tests
├── service/
│   ├── FleetServiceUnitTest.java
│   └── FleetServiceIntegrationTest.java
└── repository/
    └── FleetRepositoryTest.java
```

### Writing Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class CarModelServiceUnitTest {

    @Mock
    private CarModelRepository carModelRepository;

    @InjectMocks
    private CarModelService carModelService;

    @Test
    @DisplayName("Should return all car models")
    void shouldReturnAllCarModels() {
        // Given
        List<CarModel> expectedModels = Arrays.asList(
            createCarModel("Toyota Camry"),
            createCarModel("Honda Civic")
        );
        when(carModelRepository.findAll()).thenReturn(expectedModels);

        // When
        List<CarModel> actualModels = carModelService.getAllCarModels();

        // Then
        assertThat(actualModels).hasSize(2);
        assertThat(actualModels).containsExactlyElementsOf(expectedModels);
        verify(carModelRepository).findAll();
    }

    private CarModel createCarModel(String name) {
        return CarModel.builder()
            .modelName(name)
            .manufacturer("Test")
            .build();
    }
}
```

### Writing Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FleetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create car model when valid request")
    void shouldCreateCarModel() throws Exception {
        // Given
        CreateCarModelRequest request = CreateCarModelRequest.builder()
            .modelName("Toyota Camry")
            .manufacturer("Toyota")
            .year(2024)
            .dailyRate(new BigDecimal("85.00"))
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/fleet/models")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.modelName").value("Toyota Camry"))
            .andExpect(jsonPath("$.manufacturer").value("Toyota"));
    }
}
```

### Test Naming Convention

```
// Pattern: should<ExpectedBehavior>_when<StateUnderTest>

shouldReturnCarModel_whenValidIdProvided()
shouldThrowException_whenCarModelNotFound()
shouldCreateReservation_whenVehicleAvailable()
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CarModelServiceTest

# Run with coverage
./mvnw clean verify -P ci

# Run integration tests only
./mvnw verify -P integration-tests
```

---

## 📝 Commit Message Guidelines

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type       | Description             | Example                                  |
| ---------- | ----------------------- | ---------------------------------------- |
| `feat`     | New feature             | `feat: add vehicle reservation endpoint` |
| `fix`      | Bug fix                 | `fix: resolve double booking issue`      |
| `docs`     | Documentation           | `docs: update API documentation`         |
| `style`    | Code style (formatting) | `style: format CarModelService`          |
| `refactor` | Code refactoring        | `refactor: simplify reservation logic`   |
| `test`     | Add/update tests        | `test: add unit tests for FleetService`  |
| `chore`    | Maintenance tasks       | `chore: update dependencies`             |
| `perf`     | Performance improvement | `perf: optimize database queries`        |

### Examples

#### Simple Commit

```
feat: add vehicle availability check endpoint
```

#### Detailed Commit

```
feat: implement two-phase reservation system

- Add temporary reservation creation
- Add reservation confirmation with payment
- Add automatic expiration after 30 seconds
- Implement pessimistic locking to prevent double-booking

Closes #123
```

#### Bug Fix

```
fix: resolve race condition in vehicle booking

The reservation service was allowing concurrent bookings
of the same vehicle. Added pessimistic locking to prevent
this issue.

Fixes #456
```

### Rules

1. ✅ Use present tense ("add feature" not "added feature")
2. ✅ Use imperative mood ("move cursor to..." not "moves cursor to...")
3. ✅ Limit first line to 72 characters
4. ✅ Reference issues and pull requests
5. ❌ Don't end subject line with a period

---

## 🔄 Pull Request Process

### Before Creating a PR

- [ ] All tests pass locally
- [ ] Code follows style guidelines
- [ ] New tests added for new features
- [ ] Documentation updated
- [ ] No merge conflicts with main branch
- [ ] Commit messages follow guidelines

### PR Template

When creating a PR, include:

```markdown
## Description

Brief description of changes

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issues

Closes #123

## Testing

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Screenshots (if applicable)

## Checklist

- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added where needed
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests pass locally
```

### PR Review Process

1. **Automated Checks:** CI/CD pipeline must pass
2. **Code Review:** At least one approval required
3. **Address Feedback:** Make requested changes
4. **Merge:** Maintainer will merge after approval

### Merge Strategy

- **Feature Branches:** Squash and merge
- **Hotfix Branches:** Merge commit
- **Main Branch:** Protected, no direct commits

---

## 📚 Documentation

### What to Document

1. **Public APIs:** All public methods and classes
2. **Complex Logic:** Non-obvious algorithms or business rules
3. **Configuration:** New config properties or environment variables
4. **Setup Steps:** New dependencies or setup requirements

### JavaDoc Format

```java
/**
 * Creates a temporary reservation for a vehicle.
 *
 * <p>This method locks a vehicle for 30 seconds while the customer
 * completes payment. The reservation will automatically expire if
 * not confirmed within this timeframe.</p>
 *
 * @param request the reservation request containing model ID and dates
 * @return the created temporary reservation with expiry time
 * @throws NoVehicleAvailableException if no vehicles are available
 * @throws InvalidDateRangeException if dates are invalid
 */
public TemporaryReservation createTemporaryReservation(
        CreateReservationRequest request) {
    // Implementation
}
```

### README Updates

Update the README when:

- Adding new features
- Changing setup requirements
- Modifying API endpoints
- Updating deployment process

---

## 🐛 Issue Reporting

### Before Creating an Issue

1. **Search existing issues** to avoid duplicates
2. **Check documentation** for solutions
3. **Reproduce the issue** consistently
4. **Gather information** (logs, screenshots, environment)

### Bug Report Template

```markdown
## Bug Description

Clear description of the bug

## Steps to Reproduce

1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

## Expected Behavior

What should happen

## Actual Behavior

What actually happens

## Environment

- OS: [e.g., Windows 11]
- Java Version: [e.g., 17.0.8]
- Spring Boot Version: [e.g., 3.5.6]
- Database: [e.g., PostgreSQL 15]

## Logs
```

Paste relevant logs here

```

## Screenshots
If applicable
```

### Feature Request Template

```markdown
## Feature Description

Clear description of the feature

## Use Case

Why is this feature needed?

## Proposed Solution

How should it work?

## Alternatives Considered

Other approaches considered

## Additional Context

Any other relevant information
```

---

## 🎯 Code Review Checklist

### For Authors

- [ ] Code is self-documenting with clear variable names
- [ ] No commented-out code or debug logs
- [ ] Error handling is comprehensive
- [ ] No hard-coded values (use config)
- [ ] Security considerations addressed
- [ ] Performance implications considered

### For Reviewers

- [ ] Code follows project standards
- [ ] Tests are comprehensive and meaningful
- [ ] Documentation is clear and accurate
- [ ] No code smells or anti-patterns
- [ ] Security vulnerabilities checked
- [ ] Performance impact assessed

---

## 🏆 Recognition

Contributors who make significant contributions will be:

- Listed in the CONTRIBUTORS.md file
- Mentioned in release notes
- Invited to team discussions

---

## 📞 Getting Help

- **Slack:** #exploresg-fleet-dev
- **Email:** dev@exploresg.com
- **GitHub Discussions:** For general questions
- **GitHub Issues:** For bug reports and feature requests

---

## 📄 License

By contributing, you agree that your contributions will be licensed under the project's license.

---

**Thank you for contributing to ExploreSG Fleet Service! 🚗💨**
