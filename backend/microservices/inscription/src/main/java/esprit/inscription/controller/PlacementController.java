package esprit.inscription.controller;

import esprit.inscription.dto.CertificateAnalysisDTO;
import esprit.inscription.dto.RecommendationDTO;
import esprit.inscription.service.SubscriptionPricingService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inscription/placement")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlacementController {

    private final SubscriptionPricingService subscriptionPricingService;

    /**
     * Upload d'un certificat (PDF, image, etc.) et détection du niveau
     * à partir du CONTENU du fichier lorsque c'est possible (PDF),
     * avec repli sur le nom de fichier si nécessaire.
     */
    @PostMapping("/upload-certificate")
    public ResponseEntity<CertificateAnalysisDTO> uploadCertificate(
            @RequestParam("certificate") MultipartFile certificate) {

        if (certificate.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String fileName = certificate.getOriginalFilename() != null
                ? certificate.getOriginalFilename()
                : "certificate";

        String detectedLevel = detectLevelFromContent(certificate);
        if (detectedLevel == null) {
            detectedLevel = detectLevelFromFileName(fileName);
        }

        RecommendationDTO recommendation = subscriptionPricingService
                .getRecommendation(null, detectedLevel, null);

        CertificateAnalysisDTO dto = CertificateAnalysisDTO.builder()
                .fileName(fileName)
                .detectedLevel(detectedLevel)
                .recommendedPlanId(recommendation.getRecommendedPlanId())
                .recommendedPlanName(recommendation.getRecommendedPlanName())
                .build();

        return ResponseEntity.ok(dto);
    }

    /**
     * Essaie de détecter le niveau directement dans le texte du certificat PDF.
     */
    private String detectLevelFromContent(MultipartFile certificate) {
        try {
            String filename = certificate.getOriginalFilename() != null ? certificate.getOriginalFilename().toLowerCase() : "";
            String contentType = certificate.getContentType() != null ? certificate.getContentType().toLowerCase() : "";
            boolean isPdf = contentType.contains("pdf") || filename.endsWith(".pdf");
            if (!isPdf) {
                // Sans OCR, on ne peut pas lire le texte d'une image : on retournera null
                return null;
            }

            try (PDDocument document = Loader.loadPDF(certificate.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                if (text == null) return null;
                String upper = text.toUpperCase();

                if (containsLevelToken(upper, "C2")) return "C2";
                if (containsLevelToken(upper, "C1")) return "C1";
                if (containsLevelToken(upper, "B2")) return "B2";
                if (containsLevelToken(upper, "B1")) return "B1";
                if (containsLevelToken(upper, "A2")) return "A2";
                if (containsLevelToken(upper, "A1")) return "A1";
            }
        } catch (Exception e) {
            // En cas de problème de lecture PDF, on renvoie null et on laissera le repli sur le nom de fichier
            return null;
        }
        return null;
    }

    private boolean containsLevelToken(String text, String level) {
        // On cherche des variantes courantes : "B1", "LEVEL B1", "CEFR B1", etc.
        return text.contains(" " + level + " ")
                || text.contains("LEVEL " + level)
                || text.contains("NIVEAU " + level)
                || text.contains("CEFR " + level)
                || text.contains("(" + level + ")")
                || text.contains("[" + level + "]");
    }

    private String detectLevelFromFileName(String fileName) {
        String upper = fileName.toUpperCase();
        if (upper.contains("C2")) return "C2";
        if (upper.contains("C1")) return "C1";
        if (upper.contains("B2")) return "B2";
        if (upper.contains("B1")) return "B1";
        if (upper.contains("A2")) return "A2";
        if (upper.contains("A1")) return "A1";
        return null;
    }
}

