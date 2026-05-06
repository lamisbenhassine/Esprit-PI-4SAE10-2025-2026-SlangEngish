package esprit.notebook;

import esprit.notebook.dto.*;
import esprit.notebook.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = esprit.notebook.controller.NotebookController.class)
class NotebookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotebookService notebookService;
    @MockBean
    private GrammarService grammarService;
    @MockBean
    private DictionaryService dictionaryService;
    @MockBean
    private SummaryService summaryService;
    @MockBean
    private PronunciationCoachService pronunciationCoachService;
    @MockBean
    private GameService gameService;

    @Test
    @DisplayName("GET /notebook/notes should return list of notes")
    void listNotes() throws Exception {
        NoteDto n1 = new NoteDto();
        n1.setId(1L);
        n1.setUserId(7L);
        n1.setTitle("T1");
        n1.setContent("C1");

        Mockito.when(notebookService.listNotes(7L))
                .thenReturn(List.of(n1));

        mockMvc.perform(get("/notebook/notes")
                        .param("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("T1"));
    }

    @Test
    @DisplayName("POST /notebook/notes should create note and return 201")
    void createNote() throws Exception {
        NoteDto dto = new NoteDto();
        dto.setId(5L);
        dto.setUserId(7L);
        dto.setTitle("Created");
        dto.setContent("Body");

        Mockito.when(notebookService.create(eq(7L), eq("Created"), eq("Body")))
                .thenReturn(dto);

        String body = """
                {
                  "userId": 7,
                  "title": "Created",
                  "content": "Body"
                }
                """;

        mockMvc.perform(post("/notebook/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.userId").value(7L))
                .andExpect(jsonPath("$.title").value("Created"));
    }

    @Test
    @DisplayName("GET /notebook/ai/grammar delegates to GrammarService")
    void grammarEndpoint() throws Exception {
        GrammarResponse resp = new GrammarResponse();
        resp.setCorrectedText("fixed");
        resp.setIssuesFixed(2);

        Mockito.when(grammarService.correct("hello")).thenReturn(resp);

        String body = """
                { "text": "hello" }
                """;

        mockMvc.perform(post("/notebook/ai/grammar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctedText").value("fixed"))
                .andExpect(jsonPath("$.issuesFixed").value(2));
    }

    @Test
    @DisplayName("GET /notebook/ai/pronunciation-coach probe should return static JSON")
    void pronunciationCoachProbe() throws Exception {
        mockMvc.perform(get("/notebook/ai/pronunciation-coach"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coach").value("Smart Notebook pronunciation coach"))
                .andExpect(jsonPath("$.post").exists());
    }
}

