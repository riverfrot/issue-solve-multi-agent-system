package net.riverfrot.multiagent.user.application;

import net.riverfrot.multiagent.user.domain.User;
import net.riverfrot.multiagent.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getOrCreateUser(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseGet(() -> {
                    User newUser = User.createWithNickname(nickname);
                    return userRepository.save(newUser);
                });
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }
}