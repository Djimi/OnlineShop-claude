package com.onlineshop.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "normalized_username", nullable = false, unique = true, length = 50)
    private String normalizedUsername;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at")
    @CreationTimestamp(source = SourceType.VM)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @UpdateTimestamp(source = SourceType.VM)
    private Instant updatedAt;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    @PreUpdate
    private void normalizeUsername() {
        if (username != null) {
            this.normalizedUsername = username.toLowerCase();
        }
    }
}
