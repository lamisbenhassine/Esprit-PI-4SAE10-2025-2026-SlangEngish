package esprit.users.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.users.dto.SigninRequest;
import esprit.users.dto.SignupRequest;
import esprit.users.entity.Role;
import esprit.users.entity.Status;
import esprit.users.entity.User;
import esprit.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAuthTest {

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
    void signin_success() {
        User stored = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("B")
                .email("user@gmail.com")
                .password("encodedHash")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("secret1234", "encodedHash")).thenReturn(true);

        SigninRequest req = new SigninRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("secret1234");

        User out = userService.signin(req);
        assertEquals("user@gmail.com", out.getEmail());
    }

    @Test
    void signin_wrongPassword_throws() {
        User stored = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("B")
                .email("user@gmail.com")
                .password("encodedHash")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        SigninRequest req = new SigninRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("wrong");

        assertThrows(EntityNotFoundException.class, () -> userService.signin(req));
    }

    @Test
    void signin_inactiveAccount_throws() {
        User stored = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("B")
                .email("user@gmail.com")
                .password("encodedHash")
                .role(Role.STUDENT)
                .status(Status.INACTIVE)
                .build();
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("secret1234", "encodedHash")).thenReturn(true);

        SigninRequest req = new SigninRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("secret1234");

        assertThrows(IllegalStateException.class, () -> userService.signin(req));
    }

    @Test
    void signup_success_persistsEncodedPassword() {
        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("password12")).thenReturn("ENC");

        SignupRequest req = new SignupRequest();
        req.setFirstName("N");
        req.setLastName("W");
        req.setEmail("new@gmail.com");
        req.setPassword("password12");
        req.setRole("STUDENT");
        req.setPhone("12345678");
        req.setRecaptchaToken("tok");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        User saved = userService.signup(req);

        assertEquals(99L, saved.getId());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("ENC", captor.getValue().getPassword());
        assertEquals("new@gmail.com", captor.getValue().getEmail());
    }

    @Test
    void signup_blockedDomain_throws() {
        SignupRequest req = new SignupRequest();
        req.setFirstName("N");
        req.setLastName("W");
        req.setEmail("a@yopmail.com");
        req.setPassword("password12");
        req.setRole("STUDENT");
        req.setPhone("12345678");
        req.setRecaptchaToken("tok");

        assertThrows(IllegalArgumentException.class, () -> userService.signup(req));
    }

    @Test
    void signup_duplicateEmail_throws() {
        when(userRepository.existsByEmail("dup@gmail.com")).thenReturn(true);

        SignupRequest req = new SignupRequest();
        req.setFirstName("N");
        req.setLastName("W");
        req.setEmail("dup@gmail.com");
        req.setPassword("password12");
        req.setRole("STUDENT");
        req.setPhone("12345678");
        req.setRecaptchaToken("tok");

        assertThrows(IllegalArgumentException.class, () -> userService.signup(req));
    }

    @Test
    void signup_weakPassword_throws() {
        when(userRepository.existsByEmail("ok@gmail.com")).thenReturn(false);

        SignupRequest req = new SignupRequest();
        req.setFirstName("N");
        req.setLastName("W");
        req.setEmail("ok@gmail.com");
        req.setPassword("short");
        req.setRole("STUDENT");
        req.setPhone("12345678");
        req.setRecaptchaToken("tok");

        assertThrows(IllegalArgumentException.class, () -> userService.signup(req));
    }
}
