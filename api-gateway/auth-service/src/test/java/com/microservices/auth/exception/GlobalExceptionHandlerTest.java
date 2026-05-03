package com.microservices.auth.exception;

import com.microservices.auth.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email is required"));
        bindingResult.addError(new FieldError("request", "password", "Password is required"));

        MethodParameter parameter = new MethodParameter(
                this.getClass().getDeclaredMethod("dummyMethod", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Validation Failed");
        assertThat(response.getBody().details()).containsEntry("email", "Email is required");
        assertThat(response.getBody().details()).containsEntry("password", "Password is required");
    }

    @Test
    void handleBusiness_shouldReturnCorrectStatusAndMessage_forConflict() {
        ConflictException ex = new ConflictException("Email already registered");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Email already registered");
    }

    @Test
    void handleBusiness_shouldReturnCorrectStatusAndMessage_forUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Invalid credentials");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleGeneric_shouldReturn500WithGenericMessage() {
        Exception ex = new RuntimeException("Something unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String arg) {
    }
}
