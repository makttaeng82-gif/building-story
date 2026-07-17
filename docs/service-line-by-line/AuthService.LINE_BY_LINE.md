# AuthService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/AuthService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {
// 해설: 사용자 가입과 로그인 인증을 담당하는 서비스다.
    /*
     * 사용자 가입과 로그인 검증을 담당한다.
     *
     * 컨트롤러는 입력값을 받고, 이 서비스는 사용자명 중복, 비밀번호 해시 비교,
     * 새 Player 생성 같은 인증 규칙을 처리한다.
     */
    private final PlayerRepository playerRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    // 해설: 비밀번호를 평문으로 저장하지 않고 BCrypt 해시로 저장/검증하기 위한 인코더다.

    public AuthService(PlayerRepository playerRepository) {
    // 해설: 사용자 조회와 저장을 위해 PlayerRepository를 생성자 주입으로 받는다.
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Player register(String username, String password) {
    // 해설: 새 플레이어 계정을 생성한다.
        String normalized = normalize(username);
        // 해설: 닉네임 앞뒤 공백을 제거하고 null을 빈 문자열로 보정한다.
        if (normalized.length() < 2 || normalized.length() > 20) {
        // 해설: 닉네임 길이를 2~20자로 제한한다.
            throw new IllegalArgumentException("닉네임은 2~20자");
        }
        if (password == null || password.length() < 4) {
        // 해설: 비밀번호가 없거나 4자 미만이면 가입을 거부한다.
            throw new IllegalArgumentException("비밀번호는 4자 이상");
        }
        if (playerRepository.existsByUsername(normalized)) {
        // 해설: 같은 닉네임이 이미 존재하는지 확인한다.
            throw new IllegalArgumentException("이미 존재하는 닉네임");
        }
        return playerRepository.save(new Player(normalized, passwordEncoder.encode(password)));
        // 해설: 비밀번호를 BCrypt로 해시한 뒤 새 Player를 저장한다.
    }

    @Transactional(readOnly = true)
    public Optional<Player> authenticate(String username, String password) {
    // 해설: 로그인 요청의 닉네임과 비밀번호를 검증한다.
        String normalized = normalize(username);
        // 해설: 닉네임 앞뒤 공백을 제거하고 null을 빈 문자열로 보정한다.
        return playerRepository.findByUsername(normalized)
        // 해설: 정규화된 닉네임으로 플레이어를 찾는다.
                .filter(player -> passwordEncoder.matches(password, player.getPasswordHash()));
                // 해설: 입력 비밀번호와 저장된 BCrypt 해시가 맞는 경우에만 Optional에 남긴다.
    }

    private String normalize(String username) {
    // 해설: 사용자명이 null이어도 이후 검증이 안전하게 동작하도록 보정한다.
        return username == null ? "" : username.trim();
        // 해설: null은 빈 문자열로, null이 아니면 앞뒤 공백을 제거해 반환한다.
    }
}
```