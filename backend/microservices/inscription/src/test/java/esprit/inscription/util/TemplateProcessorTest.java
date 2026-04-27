package esprit.inscription.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateProcessorTest {

    private TemplateProcessor templateProcessor;

    @BeforeEach
    void setUp() {
        templateProcessor = new TemplateProcessor();
    }

    @Test
    void processTemplateReplacesSimpleAndNestedVariables() {
        String template = "Hello {{user.firstName}} {{user.lastName}}";
        Map<String, Object> vars = Map.of(
                "user",
                Map.of("firstName", "John", "lastName", "Doe")
        );

        String result = templateProcessor.processTemplate(template, vars);

        assertEquals("Hello John Doe", result);
    }

    @Test
    void processTemplateAppliesConditionalsAndLoops() {
        String template = "{{#if score >= 10}}PASS{{/if}}-{{#each items}}[{{@index}}:{{this}}]{{/each}}";
        Map<String, Object> vars = Map.of(
                "score", 12,
                "items", List.of("A", "B")
        );

        String result = templateProcessor.processTemplate(template, vars);

        assertEquals("PASS-[0:A][1:B]", result.trim());
    }

    @Test
    void helperMethodsFormatAndEscapeAsExpected() {
        assertEquals("&lt;b&gt;Hi&lt;/b&gt;", templateProcessor.escapeHtml("<b>Hi</b>"));
        assertEquals("abcd...", templateProcessor.truncate("abcdefghij", 7));
        String formattedAmount = templateProcessor.formatCurrency(42.5, null);
        assertTrue(formattedAmount.equals("42.50 EUR") || formattedAmount.equals("42,50 EUR"));
        assertEquals("", templateProcessor.formatDate(null, "yyyy-MM-dd"));
    }

    @Test
    void formatDateFallsBackToIsoWhenPatternIsInvalid() {
        LocalDateTime date = LocalDateTime.of(2026, 4, 27, 19, 30, 0);
        String formatted = templateProcessor.formatDate(date, "invalid-pattern");

        assertTrue(formatted.contains("2026-04-27T19:30"));
    }
}
