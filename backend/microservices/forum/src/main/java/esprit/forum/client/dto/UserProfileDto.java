package esprit.forum.client.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String englishLevel;
    private String subscriptionStatus;
    private String accountRole;
}
