package esprit.users.controller;

import esprit.users.dto.SigninRequest;
import esprit.users.dto.SignupRequest;
import esprit.users.dto.UserResponse;
import esprit.users.entity.User;
import esprit.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        User created = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromEntity(created));
    }

    @PostMapping("/signin")
    public ResponseEntity<UserResponse> signin(@Valid @RequestBody SigninRequest request) {
        User user = userService.signin(request);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}

