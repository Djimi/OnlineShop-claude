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
    void findByUsername_whenUserExists_returnsUser() {
        User user = TestDataFactory.createUser("john", "hashedPassword123");
        userRepository.save(user);

        Optional<User> result = userRepository.findByUsername("john");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john");
        assertThat(result.get().getPasswordHash()).isEqualTo("hashedPassword123");
    }

    @Test
    void findByUsername_whenUserNotFound_returnsEmpty() {
        Optional<User> result = userRepository.findByUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUsername_isCaseSensitive() {
        User user = TestDataFactory.createUser("John", "hashedPassword123");
        userRepository.save(user);

        Optional<User> resultLowercase = userRepository.findByUsername("john");
        Optional<User> resultUppercase = userRepository.findByUsername("JOHN");
        Optional<User> resultExact = userRepository.findByUsername("John");

        assertThat(resultLowercase).isEmpty();
        assertThat(resultUppercase).isEmpty();
        assertThat(resultExact).isPresent();
    }

    @Test
    void existsByUsername_whenUserExists_returnsTrue() {
        User user = TestDataFactory.createUser("jane", "hashedPassword123");
        userRepository.save(user);

        boolean exists = userRepository.existsByUsername("jane");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_whenUserNotFound_returnsFalse() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void save_whenValidUser_persistsAndGeneratesId() {
        User user = TestDataFactory.createUser("newuser", "hashedPassword123");

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
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
}
