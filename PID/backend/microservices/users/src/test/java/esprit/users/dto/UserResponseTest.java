package esprit.users.dto;

import esprit.users.entity.Role;
import esprit.users.entity.Status;
import esprit.users.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserResponseTest {

    @Test
    void fromEntity_mapsFields() {
        User user = User.builder()
                .id(5L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@test.com")
                .password("x")
                .role(Role.TUTOR)
                .status(Status.ACTIVE)
                .photoBase64("pic")
                .phone("+216123")
                .address("Addr")
                .build();

        UserResponse r = UserResponse.fromEntity(user);

        assertEquals(5L, r.getId());
        assertEquals("Jane", r.getFirstName());
        assertEquals("Doe", r.getLastName());
        assertEquals("jane@test.com", r.getEmail());
        assertEquals("TUTOR", r.getRole());
        assertEquals("ACTIVE", r.getStatus());
        assertEquals("pic", r.getPhotoBase64());
        assertEquals("+216123", r.getPhone());
        assertEquals("Addr", r.getAddress());
    }

    @Test
    void fromEntity_nullStatus_defaultsToActiveString() {
        User user = User.builder()
                .id(1L)
                .firstName("A")
                .lastName("B")
                .email("a@test.com")
                .password("x")
                .role(Role.STUDENT)
                .status(null)
                .build();

        UserResponse r = UserResponse.fromEntity(user);
        assertEquals("ACTIVE", r.getStatus());
    }
}
