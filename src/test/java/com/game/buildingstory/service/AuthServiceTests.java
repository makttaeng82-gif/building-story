package com.game.buildingstory.service;

import com.game.buildingstory.repo.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-service-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class AuthServiceTests {
    @Autowired
    private AuthService authService;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void registerPersistsNewPlayerAfterSideJobRemoval() {
        var player = authService.register("신규가입테스트", "1234");

        assertThat(playerRepository.findById(player.getId())).isPresent();
    }
}
