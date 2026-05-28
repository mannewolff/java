package org.mwolff.api.ingest.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mwolff.api.ingest.application.ResolveIngestTokenUseCase;
import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.InvalidIngestTokenException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class IngestTokenAuthFilterTest {

  private final ResolveIngestTokenUseCase useCase = mock(ResolveIngestTokenUseCase.class);
  private final IngestTokenAuthFilter filter = new IngestTokenAuthFilter(useCase);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void setsAuthenticationOnValidHeader() throws Exception {
    given(useCase.execute("tk_secret"))
        .willReturn(new IngestToken(7L, "user-1", "Pi", "h", Instant.EPOCH, Instant.EPOCH, false));

    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(IngestTokenAuthFilter.HEADER, "tk_secret");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .isInstanceOf(IngestTokenAuthentication.class);
    assertThat(
            ((IngestTokenAuthentication) SecurityContextHolder.getContext().getAuthentication())
                .getUserSub())
        .isEqualTo("user-1");
    verify(chain).doFilter(request, response);
  }

  @Test
  void clearsContextOnInvalidHeader() throws Exception {
    willThrow(new InvalidIngestTokenException("no")).given(useCase).execute("tk_bad");

    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(IngestTokenAuthFilter.HEADER, "tk_bad");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void leavesContextUntouchedWhenHeaderMissing() throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(useCase, org.mockito.Mockito.never()).execute(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void leavesContextUntouchedWhenHeaderBlank() throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(IngestTokenAuthFilter.HEADER, "  ");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(useCase, org.mockito.Mockito.never()).execute(any());
    verify(chain).doFilter(request, response);
  }
}
