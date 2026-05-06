package esprit.reclamation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AdminReponseRequest {

    @NotBlank(message = "Le statut est obligatoire")
    private String statut;

    @NotBlank(message = "La reponse admin est obligatoire")
    @Size(max = 1000, message = "La reponse admin ne doit pas depasser 1000 caracteres")
    private String reponseAdmin;

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getReponseAdmin() {
        return reponseAdmin;
    }

    public void setReponseAdmin(String reponseAdmin) {
        this.reponseAdmin = reponseAdmin;
    }
}
