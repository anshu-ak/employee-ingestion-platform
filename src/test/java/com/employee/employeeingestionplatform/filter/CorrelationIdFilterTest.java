package com.employee.employeeingestionplatform.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldUseValidCorrelationIdSuppliedByClient()
            throws ServletException, IOException {

        String suppliedCorrelationId = "request-123_test.value";

        when(request.getHeader(CorrelationIdFilter.HEADER_NAME))
                .thenReturn(suppliedCorrelationId);

        doAnswer(invocation -> {
            assertEquals(
                    suppliedCorrelationId,
                    MDC.get(CorrelationIdFilter.MDC_KEY)
            );
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        verify(response).setHeader(
                CorrelationIdFilter.HEADER_NAME,
                suppliedCorrelationId
        );

        verify(filterChain).doFilter(request, response);

        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing()
            throws ServletException, IOException {

        when(request.getHeader(CorrelationIdFilter.HEADER_NAME))
                .thenReturn(null);

        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        String generatedCorrelationId =
                captureResponseCorrelationId();

        assertDoesNotThrow(
                () -> UUID.fromString(generatedCorrelationId)
        );

        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "invalid/id",
            "invalid correlation id",
            "invalid@id"
    })
    void shouldGenerateCorrelationIdForInvalidHeader(
            String invalidCorrelationId
    ) throws ServletException, IOException {

        when(request.getHeader(CorrelationIdFilter.HEADER_NAME))
                .thenReturn(invalidCorrelationId);

        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        String generatedCorrelationId =
                captureResponseCorrelationId();

        assertAll(
                () -> assertNotEquals(
                        invalidCorrelationId,
                        generatedCorrelationId
                ),
                () -> assertDoesNotThrow(
                        () -> UUID.fromString(generatedCorrelationId)
                ),
                () -> assertNull(
                        MDC.get(CorrelationIdFilter.MDC_KEY)
                )
        );
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderExceedsMaximumLength()
            throws ServletException, IOException {

        String oversizedCorrelationId = "x".repeat(101);

        when(request.getHeader(CorrelationIdFilter.HEADER_NAME))
                .thenReturn(oversizedCorrelationId);

        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        String generatedCorrelationId =
                captureResponseCorrelationId();

        assertNotEquals(
                oversizedCorrelationId,
                generatedCorrelationId
        );

        assertDoesNotThrow(
                () -> UUID.fromString(generatedCorrelationId)
        );
    }

    @Test
    void shouldClearMdcWhenFilterChainThrowsException()
            throws ServletException, IOException {

        String suppliedCorrelationId = "request-that-fails";

        when(request.getHeader(CorrelationIdFilter.HEADER_NAME))
                .thenReturn(suppliedCorrelationId);

        doAnswer(invocation -> {
            assertEquals(
                    suppliedCorrelationId,
                    MDC.get(CorrelationIdFilter.MDC_KEY)
            );

            throw new ServletException(
                    "Controller processing failed"
            );
        }).when(filterChain).doFilter(request, response);

        ServletException exception = assertThrows(
                ServletException.class,
                () -> filter.doFilterInternal(
                        request,
                        response,
                        filterChain
                )
        );

        assertAll(
                () -> assertEquals(
                        "Controller processing failed",
                        exception.getMessage()
                ),
                () -> assertNull(
                        MDC.get(CorrelationIdFilter.MDC_KEY)
                )
        );

        verify(response).setHeader(
                CorrelationIdFilter.HEADER_NAME,
                suppliedCorrelationId
        );
    }

    private String captureResponseCorrelationId() {
        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);

        verify(response).setHeader(
                eq(CorrelationIdFilter.HEADER_NAME),
                captor.capture()
        );

        return captor.getValue();
    }
}