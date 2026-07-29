package org.xian.protfoliomanage.exception;

import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationBuildsErrorPayload() throws Exception {
        DummyRequest request = new DummyRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "request");
        bindingResult.addError(new FieldError("request", "ticker", "must not be blank"));

        Method method = DummyRequest.class.getDeclaredMethod("accept", DummyRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("message"));
        assertEquals("must not be blank", ((Map<?, ?>) response.getBody().get("errors")).get("ticker"));
    }

    @Test
    void handleBadRequestReturnsBadRequest() {
        ResponseEntity<Map<String, String>> response = handler.handleBadRequest(new IllegalArgumentException("bad"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad", response.getBody().get("message"));
    }

    @Test
    void handleUnauthorizedReturnsUnauthorized() {
        ResponseEntity<Map<String, String>> response = handler.handleUnauthorized(new IllegalStateException("no auth"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("no auth", response.getBody().get("message"));
    }

    @Test
    void handleUnexpectedReturnsGenericMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected server error", response.getBody().get("message"));
    }

    private static class DummyRequest {
        @SuppressWarnings("unused")
        void accept(@Valid DummyRequest request) {
        }
    }
}

