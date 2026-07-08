package org.mwolff.api.kanban.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mwolff.api.kanban.application.ResolveKanbanTokenUseCase;
import org.mwolff.api.kanban.domain.InvalidKanbanTokenException;
import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class KanbanTokenAuthFilterTest {

  private final ResolveKanbanTokenUseCase useCase = mock(ResolveKanbanTokenUseCase.class);
  private final KanbanTokenAuthFilter filter = new KanbanTokenAuthFilter(useCase);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void setsJwtAuthenticationOnValidHeader() throws Exception {
    given(useCase.execute("tk_secret"))
        .willReturn(
            new KanbanAccessToken(
                7L, "user-1", "Manne", "Board", "h", Instant.EPOCH, Instant.EPOCH, false));

    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(KanbanTokenAuthFilter.HEADER, "tk_secret");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isInstanceOf(JwtAuthenticationToken.class);
    final JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
    assertThat(jwtAuth.getToken().getSubject()).isEqualTo("user-1");
    assertThat(jwtAuth.getToken().getClaimAsString("preferred_username")).isEqualTo("Manne");
    assertThat(jwtAuth.getAuthorities()).contains(new SimpleGrantedAuthority("ROLE_KANBAN"));
    verify(chain).doFilter(request, response);
  }

  @Test
  void clearsContextOnInvalidHeader() throws Exception {
    willThrow(new InvalidKanbanTokenException("no")).given(useCase).execute("tk_bad");

    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(KanbanTokenAuthFilter.HEADER, "tk_bad");
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
    verify(useCase, never()).execute(any());
    verify(chain).doFilter(request, response);
  }

  @Test
  void leavesContextUntouchedWhenHeaderBlank() throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(KanbanTokenAuthFilter.HEADER, "  ");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(useCase, never()).execute(any());
    verify(chain).doFilter(request, response);
  }
}
