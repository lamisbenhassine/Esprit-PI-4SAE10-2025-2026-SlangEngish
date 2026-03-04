package org.example.evenement.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
