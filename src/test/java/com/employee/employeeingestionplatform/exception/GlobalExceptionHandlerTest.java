package com.employee.employeeingestionplatform.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        when(request.getRequestURI())
                .thenReturn("/api/test");

        MDC.put("correlationId", "test-correlation-id");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldHandleResponseStatusException() {
        ResponseStatusException exception =
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Upload not found"
                );

        ResponseEntity<ApiErrorResponse> response =
                handler.handleResponseStatus(exception, request);

        assertErrorResponse(
                response,
                404,
                "Not Found",
                "Upload not found"
        );
    }

    @Test
    void shouldUseHttpReasonWhenResponseStatusReasonIsMissing() {
        ResponseStatusException exception =
                new ResponseStatusException(HttpStatus.CONFLICT);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleResponseStatus(exception, request);

        assertErrorResponse(
                response,
                409,
                "Conflict",
                "Conflict"
        );
    }

    @Test
    void shouldHandleBodyValidationErrors() {
        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError emailError = new FieldError(
                "employeeRequest",
                "email",
                "must be a well-formed email address"
        );

        FieldError salaryError = new FieldError(
                "employeeRequest",
                "salary",
                "must be greater than zero"
        );

        when(exception.getBindingResult())
                .thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(emailError, salaryError));

        ResponseEntity<ApiErrorResponse> response =
                handler.handleBodyValidation(exception, request);

        assertErrorResponse(
                response,
                400,
                "Bad Request",
                "email: must be a well-formed email address; "
                        + "salary: must be greater than zero"
        );
    }

    @Test
    void shouldHandleConstraintViolation() {
        ConstraintViolationException exception =
                mock(ConstraintViolationException.class);

        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation =
                mock(ConstraintViolation.class);

        Path propertyPath = mock(Path.class);

        when(propertyPath.toString())
                .thenReturn("getEmployees.size");
        when(violation.getPropertyPath())
                .thenReturn(propertyPath);
        when(violation.getMessage())
                .thenReturn("must be less than or equal to 100");
        when(exception.getConstraintViolations())
                .thenReturn(Set.of(violation));

        ResponseEntity<ApiErrorResponse> response =
                handler.handleConstraintViolation(exception, request);

        assertErrorResponse(
                response,
                400,
                "Bad Request",
                "getEmployees.size: must be less than or equal to 100"
        );
    }

    @Test
    void shouldHandleParameterTypeMismatch() {
        MethodArgumentTypeMismatchException exception =
                mock(MethodArgumentTypeMismatchException.class);

        when(exception.getName()).thenReturn("trackingId");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleTypeMismatch(exception, request);

        assertErrorResponse(
                response,
                400,
                "Bad Request",
                "Invalid value for parameter: trackingId"
        );
    }

    @Test
    void shouldHandleMaximumUploadSize() {
        MaxUploadSizeExceededException exception =
                mock(MaxUploadSizeExceededException.class);

        ResponseEntity<ApiErrorResponse> response =
                handler.handleMaximumUploadSize(exception, request);

        assertErrorResponse(
                response,
                413,
                "Content Too Large",
                "The uploaded file exceeds the allowed size"
        );
    }

    @Test
    void shouldHandleUnexpectedException() {
        Exception exception =
                new RuntimeException("Database unavailable");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpectedException(exception, request);

        assertErrorResponse(
                response,
                500,
                "Internal Server Error",
                "An unexpected internal error occurred"
        );
    }

    @Test
    void shouldUseGenericErrorNameForUnknownHttpStatus() {
        HttpStatusCode unknownStatus =
                HttpStatusCode.valueOf(599);

        ResponseStatusException exception =
                new ResponseStatusException(
                        unknownStatus,
                        "Custom server error"
                );

        ResponseEntity<ApiErrorResponse> response =
                handler.handleResponseStatus(exception, request);

        assertErrorResponse(
                response,
                599,
                "HTTP Error",
                "Custom server error"
        );
    }

    private void assertErrorResponse(
            ResponseEntity<ApiErrorResponse> response,
            int expectedStatus,
            String expectedError,
            String expectedMessage
    ) {
        ApiErrorResponse body = response.getBody();

        assertNotNull(body);

        assertAll(
                () -> assertEquals(
                        expectedStatus,
                        response.getStatusCode().value()
                ),
                () -> assertEquals(
                        expectedStatus,
                        body.status()
                ),
                () -> assertEquals(
                        expectedError,
                        body.error()
                ),
                () -> assertEquals(
                        expectedMessage,
                        body.message()
                ),
                () -> assertEquals(
                        "/api/test",
                        body.path()
                ),
                () -> assertEquals(
                        "test-correlation-id",
                        body.correlationId()
                ),
                () -> assertNotNull(body.timestamp())
        );
    }

    @Test
    void shouldHandleAccessDeniedException() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleAccessDenied(
                        new AccessDeniedException(
                                "Access is denied"
                        ),
                        request
                );

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);

        assertAll(
                () -> assertEquals(
                        HttpStatus.FORBIDDEN,
                        response.getStatusCode()
                ),
                () -> assertEquals(403, body.status()),
                () -> assertEquals(
                        "Forbidden",
                        body.error()
                ),
                () -> assertEquals(
                        "You do not have permission to perform this operation",
                        body.message()
                ),
                () -> assertEquals(
                        request.getRequestURI(),
                        body.path()
                ),
                () -> assertEquals(
                        "test-correlation-id",
                        body.correlationId()
                ),
                () -> assertNotNull(body.timestamp())
        );
    }
}