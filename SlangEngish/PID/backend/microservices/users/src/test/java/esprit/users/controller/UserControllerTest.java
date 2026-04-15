package esprit.users.controller;

import esprit.users.entity.Role;
import esprit.users.entity.Status;
import esprit.users.entity.User;
import esprit.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getUserById_returnsUser() throws Exception {
        User u = User.builder()
                .id(7L)
                .firstName("F")
                .lastName("L")
                .email("u@test.com")
                .password("p")
                .role(Role.STUDENT)
                .status(Status.ACTIVE)
                .build();
        when(userService.getUserById(7L)).thenReturn(u);

        mockMvc.perform(get("/api/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@test.com"))
                .andExpect(jsonPath("$.id").value(7));
    }
}
