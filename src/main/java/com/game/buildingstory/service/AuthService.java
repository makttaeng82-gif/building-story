package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {
    private final PlayerRepository playerRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Player register(String username, String password) {
        String normalized = normalize(username);
        if (normalized.length() < 2 || normalized.length() > 20) {
            throw new IllegalArgumentException("닉네임은 2~20자");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("비밀번호는 4자 이상");
        }
        if (playerRepository.existsByUsername(normalized)) {
            throw new IllegalArgumentException("이미 존재하는 닉네임");
        }
        return playerRepository.save(new Player(normalized, passwordEncoder.encode(password)));
    }

    @Transactional(readOnly = true)
    public Optional<Player> authenticate(String username, String password) {
        String normalized = normalize(username);
        return playerRepository.findByUsername(normalized)
                .filter(player -> passwordEncoder.matches(password, player.getPasswordHash()));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim();
    }
}
