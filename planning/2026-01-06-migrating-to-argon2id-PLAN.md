# Migration Plan: Auth Service BCrypt to Argon2id (Clean Migration)

## Summary
Replace BCrypt password hashing with Argon2id using OWASP 2025 recommended parameters. **No backward compatibility** - existing BCrypt passwords will be invalidated (seed data will be updated).

## Target Configuration (OWASP 2025)
- **Algorithm:** Argon2id
- **Memory:** 47 MiB (47104 KiB)
- **Iterations:** 1
- **Parallelism:** 1
- **Target hash time:** 200-500ms

---

## Files to Modify

| File | Change |
|------|--------|
| `Auth/pom.xml` | Add BouncyCastle dependency |
| `Auth/src/main/java/com/onlineshop/auth/config/SecurityConfig.java` | Replace BCryptPasswordEncoder with Argon2PasswordEncoder |
| `Auth/init-db/02-seed-data.sql` | Update test user password hash to Argon2id format |

---

## Implementation Steps

### Task 1: Add BouncyCastle Dependency
**File:** `Auth/pom.xml`

Add after line 77 (after lombok dependency):
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.83</version>
</dependency>
```

---

### Task 2: Update SecurityConfig.java
**File:** `Auth/src/main/java/com/onlineshop/auth/config/SecurityConfig.java`

**Update import (line 9):**
```java
// Remove: import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// Add:
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
```

**Replace passwordEncoder() bean (lines 20-23):**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    // OWASP 2025: Argon2id with 47 MiB memory, 1 iteration, parallelism 1
    // Constructor: saltLength, hashLength, parallelism, memory (KiB), iterations
    return new Argon2PasswordEncoder(16, 32, 1, 47104, 1);
}
```

---

### Task 3: Update Seed Data
**File:** `Auth/init-db/02-seed-data.sql`

Generate new Argon2id hash for "testpass" and update the seed data.

**Note:** The hash must be generated programmatically. Create a small utility or use the running application to generate:
```java
Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 47104, 1);
String hash = encoder.encode("testpass");
System.out.println(hash);
```

Update SQL with the generated hash:
```sql
INSERT INTO users (username, normalized_username, password_hash)
VALUES ('testuser', 'testuser', '$argon2id$v=19$m=47104,t=1,p=1$GENERATED_SALT$GENERATED_HASH')
ON CONFLICT (username) DO NOTHING;
```

---

### Task 4: Run All Tests

```bash
# Unit tests
cd Auth && .\mvnw.cmd test

# Integration tests (will use new Argon2id encoder)
cd Auth && .\mvnw.cmd verify

# E2E tests (requires docker compose up - rebuild images first)
docker compose build
docker compose up -d
cd e2e-tests && .\mvnw.cmd test
```

---

## Hash Format Reference

| Format | Example |
|--------|---------|
| Old BCrypt | `$2a$15$F62WeSVx9CaeuMylgN52P...` |
| New Argon2id | `$argon2id$v=19$m=47104,t=1,p=1$SALT_BASE64$HASH_BASE64` |

---

## Important Notes

1. **Breaking Change:** All existing user passwords in the database will become invalid after this migration
2. **Test Users:** Seed data will be updated with new Argon2id hash
3. **No AuthService changes needed:** The `PasswordEncoder` interface is the same, so registration/login logic remains unchanged

---

## Sources
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [Argon2PasswordEncoder API](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/argon2/Argon2PasswordEncoder.html)
