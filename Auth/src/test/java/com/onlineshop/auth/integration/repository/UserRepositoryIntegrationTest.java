package com.onlineshop.auth.integration.repository;

import com.onlineshop.auth.entity.User;
import com.onlineshop.auth.integration.BaseIntegrationTest;
import com.onlineshop.auth.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Test
    void findByNormalizedUsername_whenUserExists_returnsUser() {
        User user = TestDataFactory.createUser("john", "hashedPassword123");
        userRepository.save(user);

        Optional<User> result = userRepository.findByNormalizedUsername("john");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john");
        assertThat(result.get().getPasswordHash()).isEqualTo("hashedPassword123");
    }

    @Test
    void findByNormalizedUsername_whenUserNotFound_returnsEmpty() {
        Optional<User> result = userRepository.findByNormalizedUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void findByNormalizedUsername_isCaseInsensitive() {
        User user = TestDataFactory.createUser("John", "hashedPassword123");
        userRepository.save(user);

        // All lookups should find the user via normalized username
        Optional<User> resultLowercase = userRepository.findByNormalizedUsername("john");
        Optional<User> resultUppercase = userRepository.findByNormalizedUsername("john");

        assertThat(resultLowercase).isPresent();
        assertThat(resultLowercase.get().getUsername()).isEqualTo("John"); // Original case preserved
        assertThat(resultUppercase).isPresent();
    }

    @Test
    void existsByNormalizedUsername_whenUserExists_returnsTrue() {
        User user = TestDataFactory.createUser("jane", "hashedPassword123");
        userRepository.save(user);

        boolean exists = userRepository.existsByNormalizedUsername("jane");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByNormalizedUsername_whenUserNotFound_returnsFalse() {
        boolean exists = userRepository.existsByNormalizedUsername("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByNormalizedUsername_isCaseInsensitive() {
        User user = TestDataFactory.createUser("TestUser", "hashedPassword123");
        userRepository.save(user);

        // Check via normalized username (lowercase)
        assertThat(userRepository.existsByNormalizedUsername("testuser")).isTrue();
    }

    @Test
    void save_whenValidUser_persistsAndGeneratesId() {
        User user = TestDataFactory.createUser("newuser", "hashedPassword123");

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getNormalizedUsername()).isEqualTo("newuser");
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void save_whenDuplicateUsername_throwsDataIntegrityViolation() {
        User user1 = TestDataFactory.createUser("duplicate", "hash1");
        userRepository.save(user1);

        User user2 = TestDataFactory.createUser("duplicate", "hash2");

        assertThatThrownBy(() -> userRepository.save(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_whenDuplicateNormalizedUsername_throwsDataIntegrityViolation() {
        User user1 = TestDataFactory.createUser("TestUser", "hash1");
        userRepository.save(user1);

        // Different case but same normalized username
        User user2 = TestDataFactory.createUser("testuser", "hash2");

        assertThatThrownBy(() -> userRepository.save(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
