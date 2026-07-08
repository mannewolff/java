package org.mwolff.api.ingest.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.common.token.TokenCryptoPort;
import org.mwolff.api.ingest.application.CreateIngestTokenUseCase.CreatedIngestToken;
import org.mwolff.api.ingest.domain.IngestToken;
import org.mwolff.api.ingest.domain.IngestTokenNotFoundException;
import org.mwolff.api.ingest.domain.IngestTokenPort;
import org.mwolff.api.ingest.domain.InvalidIngestTokenException;

class IngestTokenUseCasesTest {

  private final IngestTokenPort tokens = mock(IngestTokenPort.class);
  private final TokenCryptoPort crypto = mock(TokenCryptoPort.class);
  private final Clock fixed = Clock.fixed(Instant.parse("2026-05-28T10:00:00Z"), ZoneOffset.UTC);

  private static final String SUB_OWNER = "user-1";
  private static final String SUB_OTHER = "user-2";

  private static IngestToken token(long id, String userSub) {
    return new IngestToken(id, userSub, "Pi", "hashed", Instant.EPOCH, null, false);
  }

  // ----- create -------------------------------------------------------------

  @Test
  void createReturnsPlaintextAndPersistsHash() {
    given(crypto.generatePlaintext()).willReturn("tk_secret");
    given(crypto.hash("tk_secret")).willReturn("hashed");
    given(tokens.save(any())).willAnswer(inv -> withId((IngestToken) inv.getArgument(0), 7L));

    final CreatedIngestToken created =
        new CreateIngestTokenUseCase(tokens, crypto).execute(SUB_OWNER, "Pi");

    assertThat(created.plaintext()).isEqualTo("tk_secret");
    assertThat(created.token().tokenHash()).isEqualTo("hashed");
    assertThat(created.token().id()).isEqualTo(7L);
  }

  // ----- list ---------------------------------------------------------------

  @Test
  void listReturnsAllOfUser() {
    given(tokens.findAllByUser(SUB_OWNER)).willReturn(List.of(token(1L, SUB_OWNER)));

    final List<IngestToken> result = new ListIngestTokensUseCase(tokens).execute(SUB_OWNER);

    assertThat(result).hasSize(1);
  }

  // ----- revoke -------------------------------------------------------------

  @Test
  void revokePersistsRevokedFlag() {
    given(tokens.findById(1L)).willReturn(Optional.of(token(1L, SUB_OWNER)));

    new RevokeIngestTokenUseCase(tokens).execute(SUB_OWNER, 1L);

    verify(tokens).save(any());
  }

  @Test
  void revokeIsIdempotentWhenAlreadyRevoked() {
    final IngestToken alreadyRevoked = token(1L, SUB_OWNER).withRevoked();
    given(tokens.findById(1L)).willReturn(Optional.of(alreadyRevoked));

    new RevokeIngestTokenUseCase(tokens).execute(SUB_OWNER, 1L);

    verify(tokens, never()).save(any());
  }

  @Test
  void revokeThrowsForForeignToken() {
    given(tokens.findById(1L)).willReturn(Optional.of(token(1L, SUB_OWNER)));

    assertThatThrownBy(() -> new RevokeIngestTokenUseCase(tokens).execute(SUB_OTHER, 1L))
        .isInstanceOf(IngestTokenNotFoundException.class);
    verify(tokens, never()).save(any());
  }

  @Test
  void revokeThrowsWhenMissing() {
    given(tokens.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> new RevokeIngestTokenUseCase(tokens).execute(SUB_OWNER, 99L))
        .isInstanceOf(IngestTokenNotFoundException.class);
  }

  // ----- resolve ------------------------------------------------------------

  @Test
  void resolveLooksUpAndUpdatesLastUsed() {
    given(crypto.hash("tk_secret")).willReturn("hashed");
    given(tokens.findActiveByHash("hashed")).willReturn(Optional.of(token(1L, SUB_OWNER)));
    given(tokens.save(any())).willAnswer(inv -> inv.getArgument(0));

    final IngestToken result =
        new ResolveIngestTokenUseCase(tokens, crypto, fixed).execute("tk_secret");

    assertThat(result.lastUsedAt()).isEqualTo(Instant.parse("2026-05-28T10:00:00Z"));
    verify(tokens).save(any());
  }

  @Test
  void resolveThrowsForNullPlaintext() {
    assertThatThrownBy(() -> new ResolveIngestTokenUseCase(tokens, crypto, fixed).execute(null))
        .isInstanceOf(InvalidIngestTokenException.class);
  }

  @Test
  void resolveThrowsForBlankPlaintext() {
    assertThatThrownBy(() -> new ResolveIngestTokenUseCase(tokens, crypto, fixed).execute("   "))
        .isInstanceOf(InvalidIngestTokenException.class);
  }

  @Test
  void resolveThrowsWhenHashUnknown() {
    given(crypto.hash("tk_secret")).willReturn("hashed");
    given(tokens.findActiveByHash("hashed")).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> new ResolveIngestTokenUseCase(tokens, crypto, fixed).execute("tk_secret"))
        .isInstanceOf(InvalidIngestTokenException.class);
  }

  // ----- helpers ------------------------------------------------------------

  private static IngestToken withId(IngestToken token, long id) {
    return new IngestToken(
        id,
        token.userSub(),
        token.name(),
        token.tokenHash(),
        Instant.EPOCH,
        token.lastUsedAt(),
        token.revoked());
  }
}
