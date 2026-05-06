package esprit.users.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.users.dto.SigninFaceOnlyRequest;
import esprit.users.entity.Role;
import esprit.users.entity.Status;
import esprit.users.entity.User;
import esprit.users.repository.UserRepository;
import esprit.users.util.FaceEmbeddingComparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSigninFaceOnlyTest {

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

    private static List<Double> vectorOf(double fill) {
        List<Double> v = new ArrayList<>(128);
        for (int i = 0; i < 128; i++) {
            v.add(fill);
        }
        return v;
    }

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

    private User userWithFace(String email, List<Double> descriptor) throws Exception {
        return User.builder()
                .id(1L)
                .firstName("T")
                .lastName("T")
                .email(email)
                .password("encoded")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .faceDescriptorJson(objectMapper.writeValueAsString(descriptor))
                .build();
    }

    @Test
    void signinFaceOnly_noCandidates_throws() {
        SigninFaceOnlyRequest req = new SigninFaceOnlyRequest();
        req.setFaceDescriptor(vectorOf(0));
        when(userRepository.findUsersWithFaceDescriptor()).thenReturn(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> userService.signinFaceOnly(req));
    }

    @Test
    void signinFaceOnly_invalidDescriptorSize_throws() {
        SigninFaceOnlyRequest req = new SigninFaceOnlyRequest();
        req.setFaceDescriptor(Collections.singletonList(1.0));
        assertThrows(IllegalArgumentException.class, () -> userService.signinFaceOnly(req));
    }

    @Test
    void signinFaceOnly_exactMatch_returnsUser() throws Exception {
        List<Double> emb = vectorOf(0.02);
        User u = userWithFace("u@test.com", emb);
        when(userRepository.findUsersWithFaceDescriptor()).thenReturn(Collections.singletonList(u));

        SigninFaceOnlyRequest req = new SigninFaceOnlyRequest();
        req.setFaceDescriptor(new ArrayList<>(emb));

        User out = userService.signinFaceOnly(req);
        assertEquals(u.getEmail(), out.getEmail());
    }

    @Test
    void signinFaceOnly_distanceTooHigh_throws() throws Exception {
        List<Double> stored = vectorOf(0);
        List<Double> probe = vectorOf(1);
        User u = userWithFace("u@test.com", stored);
        when(userRepository.findUsersWithFaceDescriptor()).thenReturn(Collections.singletonList(u));

        SigninFaceOnlyRequest req = new SigninFaceOnlyRequest();
        req.setFaceDescriptor(probe);

        assertThrows(IllegalArgumentException.class, () -> userService.signinFaceOnly(req));
    }

    @Test
    void signinFaceOnly_twoVeryCloseMatches_throwsAmbiguous() throws Exception {
        List<Double> emb = vectorOf(0.1);
        User u1 = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("A")
                .email("a@test.com")
                .password("x")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .faceDescriptorJson(objectMapper.writeValueAsString(emb))
                .build();
        User u2 = User.builder()
                .id(2L)
                .firstName("B")
                .lastName("B")
                .email("b@test.com")
                .password("x")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .faceDescriptorJson(objectMapper.writeValueAsString(new ArrayList<>(emb)))
                .build();

        when(userRepository.findUsersWithFaceDescriptor()).thenReturn(List.of(u1, u2));

        SigninFaceOnlyRequest req = new SigninFaceOnlyRequest();
        req.setFaceDescriptor(new ArrayList<>(emb));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.signinFaceOnly(req));
        assertTrue(ex.getMessage().contains("trop proches"), ex.getMessage());
    }

    @Test
    void signinFaceOnly_skipsInactiveUsers() throws Exception {
        List<Double> emb = vectorOf(0);
        User inactive = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("A")
                .email("a@test.com")
                .password("x")
                .role(Role.STUDENT)
                .status(Status.INACTIVE)
                .faceDescriptorJson(objectMapper.writeValueAsString(emb))
                .build();
        when(userRepository.findUsersWithFaceDescriptor()).thenReturn(Collections.singletonList(inactive));

        SigninFaceOnlyRequest req = new SigninFaceOnlyRequest();
        req.setFaceDescriptor(new ArrayList<>(emb));

        assertThrows(IllegalArgumentException.class, () -> userService.signinFaceOnly(req));
    }
}
