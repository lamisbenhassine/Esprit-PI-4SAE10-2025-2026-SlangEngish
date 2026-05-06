package esprit.reclamation.dto;

/** Réponse POST /admin/backfill-ml */
public class BackfillMlResponseDto {

    private final int updated;
    private final int skipped;

    public BackfillMlResponseDto(int updated, int skipped) {
        this.updated = updated;
        this.skipped = skipped;
    }

    public int getUpdated() {
        return updated;
    }

    public int getSkipped() {
        return skipped;
    }
}
