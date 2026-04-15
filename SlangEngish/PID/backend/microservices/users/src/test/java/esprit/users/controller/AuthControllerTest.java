package esprit.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.users.dto.SigninRequest;
import esprit.users.entity.Role;
import esprit.users.entity.Status;
import esprit.users.entity.User;
import esprit.users.service.RecaptchaVerificationService;
import esprit.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private RecaptchaVerificationService recaptchaVerificationService;

    @Test
    void signin_returnsOkAndEmail() throws Exception {
        User user = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("B")
                .email("a@test.com")
                .password("x")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userService.signin(any(SigninRequest.class))).thenReturn(user);

        SigninRequest body = new SigninRequest();
        body.setEmail("a@test.com");
        body.setPassword("password12");

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@test.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void signup_recaptchaFails_returnsBadRequest() throws Exception {
        when(recaptchaVerificationService.verify(any())).thenReturn(false);

        String json = """
                {
                  "firstName": "New",
                  "lastName": "User",
                  "email": "n@gmail.com",
                  "password": "password12",
                  "role": "STUDENT",
                  "phone": "12345678",
                  "recaptchaToken": "bad"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("robot")));
    }
}
