package com.onlineshop.auth.integration.repository;

import com.onlineshop.auth.entity.Session;
import com.onlineshop.auth.entity.User;
import com.onlineshop.auth.integration.BaseIntegrationTest;
import com.onlineshop.auth.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionRepositoryIntegrationTest extends BaseIntegrationTest {

    @Test
    void findByTokenHash_whenSessionExists_returnsSession() {
        User user = userRepository.save(TestDataFactory.createUser("user1", "hash"));
        Session session = TestDataFactory.createSession("tokenHash123", user, Instant.now().plus(1, ChronoUnit.HOURS));
        sessionRepository.save(session);

        Optional<Session> result = sessionRepository.findByTokenHash("tokenHash123");

        assertThat(result).isPresent();
        assertThat(result.get().getTokenHash()).isEqualTo("tokenHash123");
    }

    @Test
    void findByTokenHash_whenSessionNotFound_returnsEmpty() {
        Optional<Session> result = sessionRepository.findByTokenHash("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findByTokenHash_loadsUserRelationship() {
        User user = userRepository.save(TestDataFactory.createUser("user2", "hash"));
        Session session = TestDataFactory.createSession("tokenHash456", user, Instant.now().plus(1, ChronoUnit.HOURS));
        sessionRepository.save(session);

        Optional<Session> result = sessionRepository.findByTokenHash("tokenHash456");

        assertThat(result).isPresent();
        assertThat(result.get().getUser()).isNotNull();
        assertThat(result.get().getUser().getUsername()).isEqualTo("user2");
    }

    @Test
    @Transactional
    void deleteExpiredSessions_whenExpiredSessionsExist_deletesOnlyExpired() {
        User user = userRepository.save(TestDataFactory.createUser("user3", "hash"));
        Instant now = Instant.now();

        Session expiredSession = TestDataFactory.createSession("expired", user, now.minus(1, ChronoUnit.HOURS));
        Session validSession = TestDataFactory.createSession("valid", user, now.plus(1, ChronoUnit.HOURS));
        sessionRepository.save(expiredSession);
        sessionRepository.save(validSession);

        sessionRepository.deleteExpiredSessions(now);

        assertThat(sessionRepository.findByTokenHash("expired")).isEmpty();
        assertThat(sessionRepository.findByTokenHash("valid")).isPresent();
    }

    @Test
    @Transactional
    void deleteExpiredSessions_whenNoExpiredSessions_deletesNothing() {
        User user = userRepository.save(TestDataFactory.createUser("user4", "hash"));
        Session validSession = TestDataFactory.createSession("valid2", user, Instant.now().plus(1, ChronoUnit.HOURS));
        sessionRepository.save(validSession);

        sessionRepository.deleteExpiredSessions(Instant.now());

        assertThat(sessionRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional
    void deleteExpiredSessions_withBoundaryTime_handlesCorrectly() {
        User user = userRepository.save(TestDataFactory.createUser("user5", "hash"));
        Instant exactTime = Instant.now();

        Session sessionAtExactTime = TestDataFactory.createSession("exact", user, exactTime);
        sessionRepository.save(sessionAtExactTime);

        sessionRepository.deleteExpiredSessions(exactTime);

        // Session at exact time should NOT be deleted (expires_at < now, not <=)
        assertThat(sessionRepository.findByTokenHash("exact")).isPresent();
    }

    @Test
    @Transactional
    void deleteByUserId_whenUserHasSessions_deletesAllUserSessions() {
        User user1 = userRepository.save(TestDataFactory.createUser("user6", "hash"));
        User user2 = userRepository.save(TestDataFactory.createUser("user7", "hash"));

        sessionRepository.save(TestDataFactory.createSession("user1session1", user1, Instant.now().plus(1, ChronoUnit.HOURS)));
        sessionRepository.save(TestDataFactory.createSession("user1session2", user1, Instant.now().plus(1, ChronoUnit.HOURS)));
        sessionRepository.save(TestDataFactory.createSession("user2session1", user2, Instant.now().plus(1, ChronoUnit.HOURS)));

        sessionRepository.deleteByUserId(user1.getId());

        assertThat(sessionRepository.findByTokenHash("user1session1")).isEmpty();
        assertThat(sessionRepository.findByTokenHash("user1session2")).isEmpty();
        assertThat(sessionRepository.findByTokenHash("user2session1")).isPresent();
    }

    @Test
    @Transactional
    void deleteByUserId_whenUserHasNoSessions_doesNothing() {
        User user = userRepository.save(TestDataFactory.createUser("user8", "hash"));

        sessionRepository.deleteByUserId(user.getId());

        assertThat(sessionRepository.count()).isZero();
    }

    @Test
    void save_whenValidSession_persistsAndGeneratesId() {
        User user = userRepository.save(TestDataFactory.createUser("user9", "hash"));
        Session session = TestDataFactory.createSession("newtoken", user, Instant.now().plus(1, ChronoUnit.HOURS));

        Session savedSession = sessionRepository.save(session);

        assertThat(savedSession.getId()).isNotNull();
        assertThat(savedSession.getTokenHash()).isEqualTo("newtoken");
        assertThat(savedSession.getCreatedAt()).isNotNull();
    }

    @Test
    void deleteUser_cascadesDeleteToSessions() {
        User user = userRepository.save(TestDataFactory.createUser("user10", "hash"));
        sessionRepository.save(TestDataFactory.createSession("cascadetoken", user, Instant.now().plus(1, ChronoUnit.HOURS)));

        userRepository.delete(user);

        assertThat(sessionRepository.findByTokenHash("cascadetoken")).isEmpty();
    }

    @Test
    void save_whenDuplicateTokenHash_throwsDataIntegrityViolation() {
        User user = userRepository.save(TestDataFactory.createUser("user11", "hash"));
        Session session1 = TestDataFactory.createSession("duplicatetoken", user, Instant.now().plus(1, ChronoUnit.HOURS));
        sessionRepository.save(session1);

        Session session2 = TestDataFactory.createSession("duplicatetoken", user, Instant.now().plus(2, ChronoUnit.HOURS));

        assertThatThrownBy(() -> sessionRepository.save(session2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
