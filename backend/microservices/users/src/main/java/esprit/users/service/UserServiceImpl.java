package esprit.users.service;

import esprit.users.dto.SigninRequest;
import esprit.users.dto.SignupRequest;
import esprit.users.entity.User;
import esprit.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public User signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.toRoleEnum())
                .build();

        return userRepository.save(user);
    }

    @Override
    public User signin(SigninRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Invalid email or password"));

        String storedPassword = user.getPassword();
        // BCrypt hashes start with $2a$, $2b$ or $2y$
        if (storedPassword == null || !storedPassword.startsWith("$2")) {
            throw new EntityNotFoundException(
                "Invalid password format. This account was created outside the application. " +
                "Please use the signup form to create a new account, or contact an administrator."
            );
        }

        if (!passwordEncoder.matches(request.getPassword(), storedPassword)) {
            throw new EntityNotFoundException("Invalid email or password");
        }

        return user;
    }

    @Override
    public User updateUser(Long id, User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        existing.setFirstName(user.getFirstName());
        existing.setLastName(user.getLastName());
        existing.setRole(user.getRole());

        if (!existing.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new IllegalArgumentException("Email is already in use");
            }
            existing.setEmail(user.getEmail());
        }

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(existing);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}

