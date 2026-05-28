package org.mwolff.api.ingest.web;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Spring-Security-Authentication-Token fuer einen authentifizierten Ingest-Request. Principal ist
 * der {@code userSub} des Token-Eigentuemers; Authority ist {@code ROLE_INGEST}, damit die Ingest-
 * Endpoints nicht versehentlich durch JWT-User-Authorities erreichbar werden.
 */
public final class IngestTokenAuthentication extends AbstractAuthenticationToken {

  private static final long serialVersionUID = 1L;

  private static final Collection<GrantedAuthority> AUTHORITIES =
      List.of(new SimpleGrantedAuthority("ROLE_INGEST"));

  private final String userSub;
  private final Long tokenId;

  public IngestTokenAuthentication(String userSub, Long tokenId) {
    super(AUTHORITIES);
    this.userSub = userSub;
    this.tokenId = tokenId;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return userSub;
  }

  public String getUserSub() {
    return userSub;
  }

  public Long getTokenId() {
    return tokenId;
  }
}
