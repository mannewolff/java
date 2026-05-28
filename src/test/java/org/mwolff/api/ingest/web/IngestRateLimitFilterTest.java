package org.mwolff.api.ingest.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mwolff.api.ingest.infrastructure.ratelimit.IngestRateLimiter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class IngestRateLimitFilterTest {

  private final IngestRateLimiter limiter = mock(IngestRateLimiter.class);
  private final IngestRateLimitFilter filter = new IngestRateLimitFilter(limiter);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usesTokenKeyWhenAuthenticated() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new IngestTokenAuthentication("user-1", 7L));
    given(limiter.tryAcquire("token:7")).willReturn(true);

    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(limiter).tryAcquire("token:7");
    verify(chain).doFilter(request, response);
  }

  @Test
  void fallsBackToIpWhenUnauthenticated() throws Exception {
    given(limiter.tryAcquire("ip:1.2.3.4")).willReturn(true);
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("1.2.3.4");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(limiter).tryAcquire("ip:1.2.3.4");
  }

  @Test
  void rejectsWith429WhenLimitExceeded() throws Exception {
    given(limiter.tryAcquire(org.mockito.ArgumentMatchers.anyString())).willReturn(false);

    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("1.2.3.4");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getContentAsString()).contains("rate limit");
    verify(chain, org.mockito.Mockito.never()).doFilter(request, response);
  }
}
