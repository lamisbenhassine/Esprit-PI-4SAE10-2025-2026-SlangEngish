package esprit.user.controller;

import esprit.user.entity.User;
import esprit.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Getting all users");
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("Getting user by ID: {}", id);
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        log.info("Getting user by email: {}", email);
        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/level/{englishLevel}")
    public ResponseEntity<List<User>> getUsersByEnglishLevel(@PathVariable String englishLevel) {
        log.info("Getting users by English level: {}", englishLevel);
        return ResponseEntity.ok(userRepository.findByEnglishLevel(englishLevel));
    }

    @GetMapping("/by-role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        log.info("Getting users by account role: {}", role);
        return ResponseEntity.ok(userRepository.findByAccountRole(role.toUpperCase()));
    }

    /** Pour le fil / forum : noms et rôles sans mot de passe (WRITE_ONLY sur User). */
    @PostMapping("/lookup")
    public ResponseEntity<List<User>> lookupByIds(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Long> distinct = ids.stream().distinct().toList();
        return ResponseEntity.ok(userRepository.findAllById(distinct));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        log.info("Creating new user: {}", user.getEmail());
        return ResponseEntity.status(201).body(userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        log.info("Updating user: {}", id);
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setFirstName(user.getFirstName());
                    existing.setLastName(user.getLastName());
                    existing.setEmail(user.getEmail());
                    existing.setPassword(user.getPassword());
                    existing.setEnglishLevel(user.getEnglishLevel());
                    existing.setSubscriptionStatus(user.getSubscriptionStatus());
                    if (user.getAccountRole() != null && !user.getAccountRole().isBlank()) {
                        existing.setAccountRole(user.getAccountRole().toUpperCase());
                    }
                    return ResponseEntity.ok(userRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: {}", id);
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

