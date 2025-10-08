# Role Enum Implementation

## Overview

Implemented a type-safe Role enum system to reduce the chance of errors when working with user roles and authorities in the Fleet Service.

## Changes Made

### 1. Created `Role` Enum (`model/Role.java`)

- Defines all available roles: `USER`, `SUPPORT`, `ADMIN`, `FLEET_MANAGER`, `MANAGER`
- Provides helper methods:
  - `getAuthority()`: Returns role with "ROLE\_" prefix for Spring Security (e.g., "ROLE_ADMIN")
  - `getRoleName()`: Returns plain role name (e.g., "ADMIN")

### 2. Created `SecurityConstants` Class (`constants/SecurityConstants.java`)

- Provides compile-time safe authority strings for use in `@PreAuthorize` annotations
- Individual role constants:
  - `HAS_ROLE_USER`
  - `HAS_ROLE_SUPPORT`
  - `HAS_ROLE_ADMIN`
  - `HAS_ROLE_FLEET_MANAGER`
  - `HAS_ROLE_MANAGER`
- Combined authority constants for multiple roles:
  - `HAS_ROLE_ADMIN_OR_FLEET_MANAGER`
  - `HAS_ROLE_ADMIN_OR_MANAGER`

### 3. Updated `FleetController`

**Before:**

```java
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
```

**After:**

```java
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
```

## Benefits

1. **Type Safety**: No more string typos like `'ROLE_ADMON'` or `'ROLE_ADMIM'`
2. **Compile-Time Checking**: IDE will catch errors immediately
3. **Autocomplete Support**: IDE can suggest available roles
4. **Refactoring-Friendly**: Easy to rename or change role names
5. **Single Source of Truth**: All roles defined in one place
6. **Consistency**: Ensures same role names across auth-service and fleet-service

## Usage Examples

### In Controllers:

```java
// Single role check
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public ResponseEntity<Void> adminOnlyEndpoint() { ... }

// Multiple roles
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN_OR_FLEET_MANAGER)
public ResponseEntity<Void> managerEndpoint() { ... }
```

### Programmatic Usage:

```java
// Get authority string
String authority = Role.ADMIN.getAuthority(); // "ROLE_ADMIN"

// Get plain role name
String roleName = Role.ADMIN.getRoleName(); // "ADMIN"
```

## Notes

- The enum should be kept in sync with the auth-service Role enum
- When adding new roles, update both the enum and SecurityConstants
- The constants use string concatenation to maintain compatibility with Spring Security's SpEL expressions

## Testing

Build successful with `./mvnw clean compile` ✅
