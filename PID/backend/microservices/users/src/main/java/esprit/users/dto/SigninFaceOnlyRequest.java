package esprit.users.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class SigninFaceOnlyRequest {

    /** Empreinte 128D (face-api.js) ; comparée à tous les comptes ayant une empreinte enregistrée. */
    @NotNull(message = "Le descripteur facial est requis.")
    @Size(min = 128, max = 128, message = "Le descripteur facial doit contenir 128 valeurs.")
    private List<Double> faceDescriptor;
}
