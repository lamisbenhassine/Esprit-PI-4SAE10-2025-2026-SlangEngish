package esprit.users.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceEmbeddingComparatorTest {

    private static List<Double> vectorOf(double value) {
        List<Double> v = new ArrayList<>(FaceEmbeddingComparator.EXPECTED_DIMENSION);
        for (int i = 0; i < FaceEmbeddingComparator.EXPECTED_DIMENSION; i++) {
            v.add(value);
        }
        return v;
    }

    @Test
    void euclideanDistance_identicalVectors_isZero() {
        List<Double> v = vectorOf(0.1);
        assertEquals(0.0, FaceEmbeddingComparator.euclideanDistance(v, v), 1e-9);
    }

    @Test
    void euclideanDistance_nullOrDifferentSize_returnsInfinity() {
        assertEquals(Double.POSITIVE_INFINITY, FaceEmbeddingComparator.euclideanDistance(null, vectorOf(0)), 0);
        assertEquals(Double.POSITIVE_INFINITY, FaceEmbeddingComparator.euclideanDistance(vectorOf(0), null), 0);
        assertEquals(Double.POSITIVE_INFINITY,
                FaceEmbeddingComparator.euclideanDistance(Collections.singletonList(1.0), vectorOf(0)), 0);
    }

    @Test
    void euclideanDistance_orthogonalUnitVectors() {
        List<Double> a = vectorOf(0);
        a.set(0, 1.0);
        List<Double> b = vectorOf(0);
        b.set(1, 1.0);
        assertEquals(Math.sqrt(2), FaceEmbeddingComparator.euclideanDistance(a, b), 1e-9);
    }

    @Test
    void isValidDimension_accepts128Only() {
        assertTrue(FaceEmbeddingComparator.isValidDimension(vectorOf(0)));
        assertFalse(FaceEmbeddingComparator.isValidDimension(null));
        assertFalse(FaceEmbeddingComparator.isValidDimension(Collections.emptyList()));
        List<Double> shortVec = new ArrayList<>();
        shortVec.add(1.0);
        assertFalse(FaceEmbeddingComparator.isValidDimension(shortVec));
    }
}
