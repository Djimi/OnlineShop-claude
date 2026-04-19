package com.onlineshop.auth.repository;

import com.onlineshop.auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByTokenHash(String tokenHash);

    @Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.tokenHash = :tokenHash")
    Optional<Session> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

    @Query("""
            SELECT s.user.id AS userId, s.user.username AS username, s.createdAt AS createdAt, s.expiresAt AS expiresAt
            FROM Session s
            WHERE s.tokenHash = :tokenHash
            """)
    Optional<SessionValidationProjection> findValidationProjectionByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    interface SessionValidationProjection {
        Long getUserId();
        String getUsername();
        Instant getCreatedAt();
        Instant getExpiresAt();
    }
}
