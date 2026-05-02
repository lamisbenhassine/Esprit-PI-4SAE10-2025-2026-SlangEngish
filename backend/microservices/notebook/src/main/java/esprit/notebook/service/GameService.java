package esprit.notebook.service;

import esprit.notebook.dto.GameDtos;
import esprit.notebook.model.Game;
import esprit.notebook.model.GameEntry;
import esprit.notebook.model.GameEntryProgress;
import esprit.notebook.repository.GameEntryProgressRepository;
import esprit.notebook.repository.GameEntryRepository;
import esprit.notebook.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GameEntryRepository gameEntryRepository;
    private final GameEntryProgressRepository progressRepository;

    @Transactional
    public GameDtos.GameDetail create(GameDtos.CreateGameRequest req) {
        if (req == null) throw new IllegalArgumentException("Missing body");
        if (req.getTeacherId() == null) throw new IllegalArgumentException("teacherId is required");
        String title = Optional.ofNullable(req.getTitle()).orElse("").trim();
        if (title.isBlank()) throw new IllegalArgumentException("title is required");
        if (req.getEntries() == null || req.getEntries().isEmpty()) throw new IllegalArgumentException("entries is required");

        Game g = new Game();
        g.setTeacherId(req.getTeacherId());
        g.setTitle(title);
        g.setDescription(Optional.ofNullable(req.getDescription()).orElse(null));
        if (req.getType() != null && !req.getType().isBlank()) {
            g.setType(req.getType().trim().toUpperCase());
        } else {
            g.setType("CROSSWORD");
        }
        if (req.getPublished() != null) g.setPublished(req.getPublished());

        // Persist game first to get id.
        final Game saved = gameRepository.save(g);

        List<GameEntry> entries = req.getEntries().stream()
                .sorted(Comparator.comparingInt(e -> Optional.ofNullable(e.getOrder()).orElse(0)))
                .map(e -> {
                    String clue = Optional.ofNullable(e.getClue()).orElse("").trim();
                    String answer = Optional.ofNullable(e.getAnswer()).orElse("").trim();
                    if (clue.isBlank()) throw new IllegalArgumentException("Each entry needs a clue");
                    String norm = normalizeAnswer(answer);
                    if (norm.isBlank()) throw new IllegalArgumentException("Each entry needs an answer (letters/numbers)");
                    if (norm.length() > 40) throw new IllegalArgumentException("Answer too long");
                    GameEntry ge = new GameEntry();
                    ge.setGame(saved);
                    ge.setEntryOrder(Optional.ofNullable(e.getOrder()).orElse(0));
                    ge.setClue(clue);
                    ge.setAnswerNorm(norm);
                    String teacherHint = Optional.ofNullable(e.getTeacherHint()).orElse("").trim();
                    ge.setTeacherHint(teacherHint.isBlank() ? null : teacherHint);
                    return ge;
                })
                .collect(Collectors.toList());

        // Generate crossword layout (row/col/direction) using answers, without exposing them in the API.
        CrosswordLayout layout = CrosswordLayout.generate(entries, 15, 15);
        for (CrosswordLayout.Placement p : layout.placements.values()) {
            GameEntry ge = p.entry;
            ge.setCwRow(p.row);
            ge.setCwCol(p.col);
            ge.setCwDir(p.dir);
        }
        // Assign crossword numbers (top-leftmost starts first).
        assignNumbers(entries);

        gameEntryRepository.saveAll(entries);
        return getDetail(saved.getId());
    }

    public List<GameDtos.GameSummary> listPublished(Instant createdAfter) {
        List<Game> games = createdAfter != null
                ? gameRepository.findByPublishedTrueAndCreatedAtAfterOrderByCreatedAtDesc(createdAfter)
                : gameRepository.findByPublishedTrueOrderByCreatedAtDesc();
        return games.stream().map(this::toSummary).collect(Collectors.toList());
    }

    public List<GameDtos.GameSummary> listForTeacher(Long teacherId) {
        if (teacherId == null) throw new IllegalArgumentException("teacherId is required");
        return gameRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public GameDtos.GameDetail getDetail(Long gameId) {
        Game g = gameRepository.findById(gameId).orElseThrow(() -> new NoSuchElementException("Game not found"));
        List<GameEntry> entries = gameEntryRepository.findByGameIdOrderByEntryOrderAsc(gameId);

        CrosswordMask mask = CrosswordMask.fromEntries(entries, 15, 15);

        List<GameDtos.GameEntryView> ev = entries.stream()
                .map(e -> new GameDtos.GameEntryView(
                        e.getId(),
                        e.getEntryOrder(),
                        e.getClue(),
                        e.getAnswerNorm() != null ? e.getAnswerNorm().length() : null,
                        e.getCwRow(),
                        e.getCwCol(),
                        e.getCwDir(),
                        e.getCwNumber()
                ))
                .collect(Collectors.toList());

        return new GameDtos.GameDetail(
                g.getId(),
                g.getTitle(),
                g.getDescription(),
                g.getType(),
                g.getCreatedAt(),
                g.getTeacherId(),
                mask.rows,
                mask.cols,
                mask.maskRows,
                ev
        );
    }

    public List<GameDtos.ProgressRow> progress(Long userId, Long gameId) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (gameId == null) throw new IllegalArgumentException("gameId is required");
        return progressRepository.findByUserIdAndGameId(userId, gameId).stream()
                .map(p -> new GameDtos.ProgressRow(p.getEntryId(), p.isSolved(), p.getAttempts(), p.getHintLevel(), p.getLastAnswer()))
                .collect(Collectors.toList());
    }

    @Transactional
    public GameDtos.SubmitAnswerResponse submitAnswer(Long gameId, Long entryId, GameDtos.SubmitAnswerRequest req) {
        if (req == null) throw new IllegalArgumentException("Missing body");
        if (req.getUserId() == null) throw new IllegalArgumentException("userId is required");
        GameEntry entry = loadEntry(gameId, entryId);
        Game g = gameRepository.findById(gameId).orElseThrow(() -> new NoSuchElementException("Game not found"));
        if (!g.isPublished()) throw new IllegalArgumentException("Game is not published");

        GameEntryProgress p = progressRepository.findByUserIdAndEntryId(req.getUserId(), entryId).orElseGet(() -> {
            GameEntryProgress np = new GameEntryProgress();
            np.setUserId(req.getUserId());
            np.setGameId(gameId);
            np.setEntryId(entryId);
            return np;
        });

        if (p.isSolved()) {
            return new GameDtos.SubmitAnswerResponse(true, true, p.getAttempts(), p.getHintLevel());
        }

        String norm = normalizeAnswer(req.getAnswer());
        p.setAttempts(p.getAttempts() + 1);
        p.setLastAnswer(Optional.ofNullable(req.getAnswer()).orElse(null));
        boolean correct = !norm.isBlank() && norm.equals(entry.getAnswerNorm());
        if (correct) {
            p.setSolved(true);
            p.setSolvedAt(Instant.now());
        }
        progressRepository.save(p);
        return new GameDtos.SubmitAnswerResponse(correct, p.isSolved(), p.getAttempts(), p.getHintLevel());
    }

    @Transactional
    public GameDtos.HintResponse nextHint(Long gameId, Long entryId, Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        GameEntry entry = loadEntry(gameId, entryId);
        Game g = gameRepository.findById(gameId).orElseThrow(() -> new NoSuchElementException("Game not found"));
        if (!g.isPublished()) throw new IllegalArgumentException("Game is not published");

        GameEntryProgress p = progressRepository.findByUserIdAndEntryId(userId, entryId).orElseGet(() -> {
            GameEntryProgress np = new GameEntryProgress();
            np.setUserId(userId);
            np.setGameId(gameId);
            np.setEntryId(entryId);
            return np;
        });
        if (p.isSolved()) {
            return new GameDtos.HintResponse(p.getHintLevel(), "Already solved. Try the next clue.");
        }

        int nextLevel = Math.min(6, p.getHintLevel() + 1);
        p.setHintLevel(nextLevel);
        progressRepository.save(p);

        return new GameDtos.HintResponse(nextLevel, buildHint(entry, nextLevel));
    }

    private GameEntry loadEntry(Long gameId, Long entryId) {
        if (gameId == null) throw new IllegalArgumentException("gameId is required");
        if (entryId == null) throw new IllegalArgumentException("entryId is required");
        GameEntry e = gameEntryRepository.findById(entryId).orElseThrow(() -> new NoSuchElementException("Entry not found"));
        if (e.getGame() == null || e.getGame().getId() == null || !e.getGame().getId().equals(gameId)) {
            throw new NoSuchElementException("Entry not found");
        }
        return e;
    }

    private GameDtos.GameSummary toSummary(Game g) {
        int count = 0;
        try {
            count = g.getEntries() != null ? g.getEntries().size() : 0;
        } catch (Exception ignored) {
            // lazy collections may not be initialized; fall back to 0, UI will refetch detail anyway
        }
        return new GameDtos.GameSummary(
                g.getId(),
                g.getTitle(),
                g.getDescription(),
                g.getType(),
                g.isPublished(),
                count,
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getTeacherId()
        );
    }

    static String normalizeAnswer(String raw) {
        if (raw == null) return "";
        String t = raw.trim().toUpperCase();
        // Keep letters & digits only (works for slang like "4REAL").
        t = t.replaceAll("[^A-Z0-9]", "");
        return t;
    }

    private String buildHint(GameEntry e, int level) {
        String ans = Optional.ofNullable(e.getAnswerNorm()).orElse("");
        String clue = Optional.ofNullable(e.getClue()).orElse("");
        int len = ans.length();
        String teacherHint = Optional.ofNullable(e.getTeacherHint()).orElse("").trim();

        return switch (level) {
            case 1 -> !teacherHint.isBlank()
                    ? teacherHint
                    : "Length hint: " + len + " letters.";
            case 2 -> "First letter: " + (len > 0 ? ans.charAt(0) : "?") + " (" + len + " letters).";
            case 3 -> "Last letter: " + (len > 0 ? ans.charAt(len - 1) : "?") + ".";
            case 4 -> "Reveal: " + revealPattern(ans, Math.max(1, len / 3));
            case 5 -> "Shuffle hint: " + shuffleLetters(ans) + " (anagram).";
            default -> "Mega hint: Think of \"" + clip(clue, 80) + "\". Answer starts with " + (len > 0 ? ans.charAt(0) : "?") + " and ends with " + (len > 0 ? ans.charAt(len - 1) : "?") + ".";
        };
    }

    private String revealPattern(String ans, int revealCount) {
        if (ans == null || ans.isBlank()) return "?";
        int len = ans.length();
        revealCount = Math.max(1, Math.min(len, revealCount));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            boolean reveal = i < revealCount || i == len - 1; // little help + last char tease
            sb.append(reveal ? ans.charAt(i) : '•');
        }
        return sb.toString();
    }

    private String shuffleLetters(String ans) {
        if (ans == null) return "";
        char[] a = ans.toCharArray();
        for (int i = a.length - 1; i > 0; i--) {
            int j = (int) Math.floor(Math.random() * (i + 1));
            char tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return new String(a);
    }

    private String clip(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)).trim() + "…";
    }

    private void assignNumbers(List<GameEntry> entries) {
        // Number by position: smallest row then col first; across before down for same cell.
        List<GameEntry> placed = entries.stream()
                .filter(e -> e.getCwRow() != null && e.getCwCol() != null && e.getCwDir() != null)
                .sorted((a, b) -> {
                    int ra = a.getCwRow();
                    int rb = b.getCwRow();
                    if (ra != rb) return Integer.compare(ra, rb);
                    int ca = a.getCwCol();
                    int cb = b.getCwCol();
                    if (ca != cb) return Integer.compare(ca, cb);
                    String da = Optional.ofNullable(a.getCwDir()).orElse("ACROSS");
                    String db = Optional.ofNullable(b.getCwDir()).orElse("ACROSS");
                    return da.compareTo(db);
                })
                .collect(Collectors.toList());
        int n = 1;
        Map<String, Integer> cellToNumber = new HashMap<>();
        for (GameEntry e : placed) {
            String key = e.getCwRow() + ":" + e.getCwCol();
            Integer existing = cellToNumber.get(key);
            if (existing == null) {
                cellToNumber.put(key, n);
                e.setCwNumber(n);
                n++;
            } else {
                e.setCwNumber(existing);
            }
        }
    }

    static class CrosswordMask {
        final int rows;
        final int cols;
        final List<String> maskRows;

        CrosswordMask(int rows, int cols, List<String> maskRows) {
            this.rows = rows;
            this.cols = cols;
            this.maskRows = maskRows;
        }

        static CrosswordMask fromEntries(List<GameEntry> entries, int rows, int cols) {
            boolean[][] used = new boolean[rows][cols];
            for (GameEntry e : entries) {
                if (e.getCwRow() == null || e.getCwCol() == null || e.getCwDir() == null) continue;
                String ans = Optional.ofNullable(e.getAnswerNorm()).orElse("");
                int r = e.getCwRow();
                int c = e.getCwCol();
                boolean across = "ACROSS".equalsIgnoreCase(e.getCwDir());
                for (int i = 0; i < ans.length(); i++) {
                    int rr = r + (across ? 0 : i);
                    int cc = c + (across ? i : 0);
                    if (rr >= 0 && rr < rows && cc >= 0 && cc < cols) {
                        used[rr][cc] = true;
                    }
                }
            }
            List<String> out = new java.util.ArrayList<>();
            for (int r = 0; r < rows; r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < cols; c++) sb.append(used[r][c] ? '1' : '0');
                out.add(sb.toString());
            }
            return new CrosswordMask(rows, cols, out);
        }
    }

    static class CrosswordLayout {
        final int rows;
        final int cols;
        final char[][] grid;
        final Map<Long, Placement> placements = new HashMap<>();

        CrosswordLayout(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            this.grid = new char[rows][cols];
            for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) grid[r][c] = '\0';
        }

        static CrosswordLayout generate(List<GameEntry> entries, int rows, int cols) {
            CrosswordLayout L = new CrosswordLayout(rows, cols);
            // Place longest first to increase intersections.
            List<GameEntry> sorted = entries.stream()
                    .sorted((a, b) -> Integer.compare(
                            Optional.ofNullable(b.getAnswerNorm()).orElse("").length(),
                            Optional.ofNullable(a.getAnswerNorm()).orElse("").length()
                    ))
                    .collect(Collectors.toList());
            if (sorted.isEmpty()) return L;

            GameEntry first = sorted.get(0);
            String w0 = Optional.ofNullable(first.getAnswerNorm()).orElse("");
            int startR = rows / 2;
            int startC = Math.max(0, (cols - w0.length()) / 2);
            L.place(first, startR, startC, "ACROSS");

            for (int i = 1; i < sorted.size(); i++) {
                GameEntry e = sorted.get(i);
                String w = Optional.ofNullable(e.getAnswerNorm()).orElse("");
                if (w.isBlank()) continue;
                Placement best = L.findBestPlacement(e, w);
                if (best != null) {
                    L.place(e, best.row, best.col, best.dir);
                } else {
                    // fallback: find first empty slot across
                    boolean placed = false;
                    for (int r = 0; r < rows && !placed; r++) {
                        for (int c = 0; c + w.length() <= cols && !placed; c++) {
                            if (L.canPlace(w, r, c, "ACROSS")) {
                                L.place(e, r, c, "ACROSS");
                                placed = true;
                            }
                        }
                    }
                    if (!placed) {
                        // last resort: down
                        for (int c = 0; c < cols && !placed; c++) {
                            for (int r = 0; r + w.length() <= rows && !placed; r++) {
                                if (L.canPlace(w, r, c, "DOWN")) {
                                    L.place(e, r, c, "DOWN");
                                    placed = true;
                                }
                            }
                        }
                    }
                }
            }
            return L;
        }

        private Placement findBestPlacement(GameEntry e, String w) {
            Placement best = null;
            int bestScore = -1;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    char g = grid[r][c];
                    if (g == '\0') continue;
                    for (int i = 0; i < w.length(); i++) {
                        if (w.charAt(i) != g) continue;
                        // try across intersecting at (r,c) with letter i
                        int sr = r;
                        int sc = c - i;
                        if (sc >= 0 && sc + w.length() <= cols && canPlace(w, sr, sc, "ACROSS")) {
                            int score = intersections(w, sr, sc, "ACROSS");
                            if (score > bestScore) {
                                bestScore = score;
                                best = new Placement(e, sr, sc, "ACROSS");
                            }
                        }
                        // try down
                        sr = r - i;
                        sc = c;
                        if (sr >= 0 && sr + w.length() <= rows && canPlace(w, sr, sc, "DOWN")) {
                            int score = intersections(w, sr, sc, "DOWN");
                            if (score > bestScore) {
                                bestScore = score;
                                best = new Placement(e, sr, sc, "DOWN");
                            }
                        }
                    }
                }
            }
            return best;
        }

        private int intersections(String w, int r, int c, String dir) {
            boolean across = "ACROSS".equalsIgnoreCase(dir);
            int score = 0;
            for (int i = 0; i < w.length(); i++) {
                int rr = r + (across ? 0 : i);
                int cc = c + (across ? i : 0);
                if (grid[rr][cc] == w.charAt(i)) score++;
            }
            return score;
        }

        private boolean canPlace(String w, int r, int c, String dir) {
            boolean across = "ACROSS".equalsIgnoreCase(dir);
            // boundary
            if (across) {
                if (c < 0 || c + w.length() > cols || r < 0 || r >= rows) return false;
            } else {
                if (r < 0 || r + w.length() > rows || c < 0 || c >= cols) return false;
            }

            for (int i = 0; i < w.length(); i++) {
                int rr = r + (across ? 0 : i);
                int cc = c + (across ? i : 0);
                char existing = grid[rr][cc];
                char ch = w.charAt(i);
                if (existing != '\0' && existing != ch) return false;

                // basic adjacency rule: don't touch side-by-side unless crossing on that cell
                if (existing == '\0') {
                    if (across) {
                        if (rr > 0 && grid[rr - 1][cc] != '\0') return false;
                        if (rr < rows - 1 && grid[rr + 1][cc] != '\0') return false;
                    } else {
                        if (cc > 0 && grid[rr][cc - 1] != '\0') return false;
                        if (cc < cols - 1 && grid[rr][cc + 1] != '\0') return false;
                    }
                }
            }
            // check before/after cell in the direction
            int br = r - (across ? 0 : 1);
            int bc = c - (across ? 1 : 0);
            if (br >= 0 && br < rows && bc >= 0 && bc < cols && grid[br][bc] != '\0') return false;

            int ar = r + (across ? 0 : w.length());
            int ac = c + (across ? w.length() : 0);
            if (ar >= 0 && ar < rows && ac >= 0 && ac < cols && grid[ar][ac] != '\0') return false;

            return true;
        }

        private void place(GameEntry e, int r, int c, String dir) {
            String w = Optional.ofNullable(e.getAnswerNorm()).orElse("");
            boolean across = "ACROSS".equalsIgnoreCase(dir);
            for (int i = 0; i < w.length(); i++) {
                int rr = r + (across ? 0 : i);
                int cc = c + (across ? i : 0);
                grid[rr][cc] = w.charAt(i);
            }
            placements.put(e.getId() != null ? e.getId() : System.identityHashCode(e) * 1L, new Placement(e, r, c, dir));
        }

        static class Placement {
            final GameEntry entry;
            final int row;
            final int col;
            final String dir;

            Placement(GameEntry entry, int row, int col, String dir) {
                this.entry = entry;
                this.row = row;
                this.col = col;
                this.dir = dir;
            }
        }
    }
}

