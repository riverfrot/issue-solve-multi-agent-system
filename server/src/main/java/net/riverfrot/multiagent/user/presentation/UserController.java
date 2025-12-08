package net.riverfrot.multiagent.user.presentation;

import net.riverfrot.multiagent.user.application.UserService;
import net.riverfrot.multiagent.user.domain.User;
import net.riverfrot.multiagent.user.dto.UserRequest;
import net.riverfrot.multiagent.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> loginWithNickname(@RequestBody UserRequest request) {
        User user = userService.getOrCreateUser(request.nickname());
        return ResponseEntity.ok(UserResponse.from(user));
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }
}