package net.riverfrot.multiagent.user.application;

import net.riverfrot.multiagent.user.domain.User;
import net.riverfrot.multiagent.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User 서비스
 * 최소한의 닉네임 기반 사용자 관리
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 닉네임으로 사용자 생성 또는 조회
     * 같은 닉네임이 있으면 기존 사용자 반환, 없으면 새로 생성
     */
    @Transactional
    public User getOrCreateUser(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseGet(() -> {
                    User newUser = User.createWithNickname(nickname);
                    return userRepository.save(newUser);
                });
    }
    
    /**
     * 사용자 조회 (ID로)
     */
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }
}