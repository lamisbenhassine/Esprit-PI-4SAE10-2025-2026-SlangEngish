package esprit.users.util;

import java.util.List;

/**
 * Compare les vecteurs 128D produits par face-api.js (faceRecognitionNet).
 * Une distance euclidienne plus faible indique une plus grande similarité (même personne : souvent inférieure à 0,5).
 */
public final class FaceEmbeddingComparator {

    public static final int EXPECTED_DIMENSION = 128;
    /** Seuil par défaut (face-api.js / L2 distance sur descripteur normalisé). */
    public static final double DEFAULT_MAX_DISTANCE = 0.55;

    private FaceEmbeddingComparator() {
    }

    public static double euclideanDistance(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.size() != b.size()) {
            return Double.POSITIVE_INFINITY;
        }
        double sum = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double d = a.get(i) - b.get(i);
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    public static boolean isValidDimension(List<Double> descriptor) {
        return descriptor != null && descriptor.size() == EXPECTED_DIMENSION;
    }
}
