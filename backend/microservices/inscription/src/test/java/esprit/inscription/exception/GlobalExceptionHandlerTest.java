package esprit.inscription.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericException_shouldReturnInternalServerErrorPayload() {
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(new Exception("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().get("message"));
    }

    @Test
    void handleRuntimeException_shouldReturnBadRequestPayload() {
        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(new RuntimeException("invalid state"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Bad Request", response.getBody().get("error"));
        assertEquals("invalid state", response.getBody().get("message"));
    }

    @Test
    void handleIllegalArgumentException_shouldReturnInvalidArgumentPayload() {
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Invalid Argument", response.getBody().get("error"));
        assertEquals("bad input", response.getBody().get("message"));
    }
}
