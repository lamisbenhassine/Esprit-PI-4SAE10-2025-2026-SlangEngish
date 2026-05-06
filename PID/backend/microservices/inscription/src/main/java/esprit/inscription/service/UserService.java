package esprit.inscription.service;

import esprit.inscription.entity.User;
import esprit.inscription.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        log.info("Getting all users");
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        log.info("Getting user by ID: {}", id);
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    public List<User> getUsersByEnglishLevel(String englishLevel) {
        log.info("Getting users by English level: {}", englishLevel);
        return userRepository.findByEnglishLevel(englishLevel);
    }

    @Transactional
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getEmail());
        
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }
        
        // Set default values
        if (user.getSubscriptionStatus() == null) {
            user.setSubscriptionStatus("TRIAL");
        }
        
        if (user.getEnglishLevel() == null) {
            user.setEnglishLevel("A1");
        }
        
        User savedUser = userRepository.save(user);
        log.info("Successfully created user with ID: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public User updateUser(Long id, User user) {
        log.info("Updating user: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        
        // Update fields
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setEnglishLevel(user.getEnglishLevel());
        existingUser.setSubscriptionStatus(user.getSubscriptionStatus());
        existingUser.setTrialEndsAt(user.getTrialEndsAt());
        
        User updatedUser = userRepository.save(existingUser);
        log.info("Successfully updated user: {}", id);
        return updatedUser;
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        
        userRepository.deleteById(id);
        log.info("Successfully deleted user: {}", id);
    }

    public List<User> getActiveUsersByLevel(String level) {
        log.info("Getting active users by level: {}", level);
        return userRepository.findActiveUsersByLevel(level);
    }

    public List<User> getInactiveUsersSince(LocalDateTime cutoffDate) {
        log.info("Getting inactive users since: {}", cutoffDate);
        return userRepository.findInactiveUsersSince(cutoffDate);
    }

    public List<User> getHighValueUsers(BigDecimal minRevenue) {
        log.info("Getting high value users with min revenue: {}", minRevenue);
        return userRepository.findHighValueUsers(minRevenue);
    }

    public List<User> getUsersWithExpiringTrials(LocalDateTime now, LocalDateTime futureDate) {
        log.info("Getting users with expiring trials between {} and {}", now, futureDate);
        return userRepository.findUsersWithExpiringTrials(now);
    }

    public Long countUsersByLevel(String level) {
        log.info("Counting users by level: {}", level);
        return userRepository.countUsersByLevel(level);
    }

    public Long countActiveUsers() {
        log.info("Counting active users");
        return userRepository.countActiveUsers();
    }
}
