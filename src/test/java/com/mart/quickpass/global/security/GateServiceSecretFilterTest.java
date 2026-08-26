package com.mart.quickpass.global.security;

import com.mart.quickpass.global.config.GateProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GateServiceSecretFilterTest {

    private final GateServiceSecretFilter filter =
            new GateServiceSecretFilter(new GateProperties("correct-secret"), new ObjectMapper());

    @Test
    void missingSecretIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/internal/gate/inspections");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        verifyNoInteractions(chain);
    }

    @Test
    void correctSecretAllowsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/internal/gate/inspections");
        request.addHeader(GateServiceSecretFilter.HEADER_NAME, "correct-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
