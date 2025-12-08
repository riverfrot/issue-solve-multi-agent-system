package net.riverfrot.multiagent.user.application;

import net.riverfrot.multiagent.user.domain.User;
import net.riverfrot.multiagent.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("UserService 테스트")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("기존 닉네임이 있으면 기존 사용자 반환")
    void getOrCreateUser_shouldReturnExistingUserWhenNicknameExists() {
        // Given
        String nickname = "existingUser";
        User existingUser = User.createWithNickname(nickname);
        given(userRepository.findByNickname(nickname)).willReturn(Optional.of(existingUser));

        // When
        User result = userService.getOrCreateUser(nickname);

        // Then
        assertThat(result).isEqualTo(existingUser);
        then(userRepository).should().findByNickname(nickname);
        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("새로운 닉네임이면 새 사용자 생성")
    void getOrCreateUser_shouldCreateNewUserWhenNicknameNotExists() {
        // Given
        String nickname = "newUser";
        User newUser = User.createWithNickname(nickname);
        given(userRepository.findByNickname(nickname)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(newUser);

        // When
        User result = userService.getOrCreateUser(nickname);

        // Then
        assertThat(result.getNickname()).isEqualTo(nickname);
        then(userRepository).should().findByNickname(nickname);
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID 조회시 빈 Optional 반환")
    void findById_shouldReturnEmptyWhenUserNotFound() {
        // Given
        String nonExistentId = "nonExistent";
        given(userRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // When
        Optional<User> result = userService.findById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
        then(userRepository).should().findById(nonExistentId);
    }

    @Test
    @DisplayName("유효한 사용자 ID 조회시 사용자 반환")
    void findById_shouldReturnUserWhenExists() {
        // Given
        String userId = "validId";
        User user = User.createWithNickname("testUser");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // When
        Optional<User> result = userService.findById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
        then(userRepository).should().findById(userId);
    }
}