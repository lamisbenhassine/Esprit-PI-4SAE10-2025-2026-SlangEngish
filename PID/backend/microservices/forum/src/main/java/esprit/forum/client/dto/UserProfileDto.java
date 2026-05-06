package esprit.forum.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String englishLevel;
    private String subscriptionStatus;
    /** Mappé depuis le champ JSON {@code role} du user-service. */
    @JsonProperty("role")
    private String accountRole;
}
