package esprit.notebook.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "notebook_game_entries",
        indexes = {
                @Index(name = "idx_entry_game", columnList = "game_id"),
                @Index(name = "idx_entry_game_order", columnList = "game_id, entry_order")
        }
)
public class GameEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "entry_order", nullable = false)
    private Integer entryOrder = 0;

    @Column(nullable = false, length = 240)
    private String clue;

    /** Stored uppercase, no spaces/punct. */
    @Column(name = "answer_norm", nullable = false, length = 40)
    private String answerNorm;

    /** Optional teacher-written hint (first hint level). */
    @Column(name = "teacher_hint", length = 240)
    private String teacherHint;

    // ----- Crossword layout (generated on create) -----
    /** 0-based row in crossword grid. */
    @Column(name = "cw_row")
    private Integer cwRow;

    /** 0-based col in crossword grid. */
    @Column(name = "cw_col")
    private Integer cwCol;

    /** 'ACROSS' or 'DOWN'. */
    @Column(name = "cw_dir", length = 8)
    private String cwDir;

    /** Display number in crossword (1..N). */
    @Column(name = "cw_number")
    private Integer cwNumber;
}

