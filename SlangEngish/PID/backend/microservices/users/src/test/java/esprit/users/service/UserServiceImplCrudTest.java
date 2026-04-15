package esprit.users.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.users.entity.Role;
import esprit.users.entity.Status;
import esprit.users.entity.User;
import esprit.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplCrudTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetEmailService passwordResetEmailService;
    @Mock
    private PasswordResetWhatsAppService passwordResetWhatsAppService;
    @Mock
    private AccountStatusEmailService accountStatusEmailService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                passwordEncoder,
                passwordResetEmailService,
                passwordResetWhatsAppService,
                accountStatusEmailService,
                objectMapper
        );
    }

    @Test
    void getUserById_found() {
        User u = User.builder()
                .id(3L)
                .firstName("A")
                .lastName("B")
                .email("a@test.com")
                .password("p")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));

        assertEquals(3L, userService.getUserById(3L).getId());
    }

    @Test
    void getUserById_missing_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void deleteUser_nonAdmin_throws() {
        User tutor = User.builder()
                .id(10L)
                .firstName("T")
                .lastName("T")
                .email("t@test.com")
                .password("p")
                .role(Role.TUTOR)
                .status(Status.ACTIVE)
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(tutor));

        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(5L, 10L));
    }

    @Test
    void deleteUser_adminDeletesStudent_ok() {
        User admin = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("A")
                .email("admin@test.com")
                .password("p")
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();
        User student = User.builder()
                .id(5L)
                .firstName("S")
                .lastName("S")
                .email("s@test.com")
                .password("p")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));

        userService.deleteUser(5L, 1L);

        verify(userRepository).delete(student);
    }

    @Test
    void resetPassword_invalidToken_throws() {
        when(userRepository.findByResetToken("bad")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userService.resetPassword("bad", "password12"));
    }

    @Test
    void resetPassword_expiredToken_throws() {
        User u = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("B")
                .email("a@test.com")
                .password("old")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .resetToken("tok")
                .resetTokenExpiry(LocalDateTime.now().minusHours(1))
                .build();
        when(userRepository.findByResetToken("tok")).thenReturn(Optional.of(u));

        assertThrows(IllegalStateException.class, () -> userService.resetPassword("tok", "password12"));
    }

    @Test
    void resetPassword_success() {
        User u = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("B")
                .email("a@test.com")
                .password("old")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .resetToken("tok")
                .resetTokenExpiry(LocalDateTime.now().plusHours(1))
                .build();
        when(userRepository.findByResetToken("tok")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("password12")).thenReturn("NEW");

        userService.resetPassword("tok", "password12");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void getAllUsers_delegatesToRepository() {
        when(userRepository.findAll()).thenReturn(List.of());
        assertEquals(0, userService.getAllUsers().size());
    }
}
