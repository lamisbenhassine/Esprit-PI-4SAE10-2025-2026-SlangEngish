package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateAnalysisDTO {

    private String fileName;
    /**
     * Niveau détecté à partir du certificat (A1, A2, B1, B2, C1…).
     */
    private String detectedLevel;

    /**
     * Plan recommandé (issu du moteur de tarification).
     */
    private Long recommendedPlanId;
    private String recommendedPlanName;
}

