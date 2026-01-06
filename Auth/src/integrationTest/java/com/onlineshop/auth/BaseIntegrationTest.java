package com.onlineshop.auth;

import com.onlineshop.auth.repository.SessionRepository;
import com.onlineshop.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected SessionRepository sessionRepository;

    @BeforeEach
    void cleanDatabase() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }
}
