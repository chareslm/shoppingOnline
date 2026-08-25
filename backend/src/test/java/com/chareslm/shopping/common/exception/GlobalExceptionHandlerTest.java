package com.chareslm.shopping.common.exception;

import com.chareslm.shopping.common.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void shouldMapMissingRequestParameterToBadRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id")).thenReturn("trace-parameter");

        var response = new GlobalExceptionHandler().handleRequestConstraint(
                new MissingServletRequestParameterException("startAt", "LocalDateTime"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.VALIDATION_ERROR.code(), response.getBody().code());
        assertEquals("缺少必填参数：startAt", response.getBody().message());
        assertEquals("trace-parameter", response.getBody().traceId());
    }
}
