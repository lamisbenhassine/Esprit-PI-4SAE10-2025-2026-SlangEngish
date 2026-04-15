package esprit.users.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class SigninFaceRequest {

    @NotBlank(message = "L'email est requis.")
    @Email(message = "Email invalide.")
    private String email;

    /** Empreinte 128D (face-api.js), même format qu'à l'inscription. */
    @NotNull(message = "Le descripteur facial est requis.")
    @Size(min = 128, max = 128, message = "Le descripteur facial doit contenir 128 valeurs.")
    private List<Double> faceDescriptor;
}
