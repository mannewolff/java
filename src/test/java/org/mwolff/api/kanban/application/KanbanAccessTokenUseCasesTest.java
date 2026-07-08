package org.mwolff.api.kanban.application;

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
import org.mwolff.api.kanban.application.CreateKanbanTokenUseCase.CreatedKanbanToken;
import org.mwolff.api.kanban.domain.InvalidKanbanTokenException;
import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.mwolff.api.kanban.domain.KanbanAccessTokenPort;
import org.mwolff.api.kanban.domain.KanbanTokenNotFoundException;

class KanbanAccessTokenUseCasesTest {

  private final KanbanAccessTokenPort tokens = mock(KanbanAccessTokenPort.class);
  private final TokenCryptoPort crypto = mock(TokenCryptoPort.class);
  private final Clock fixed = Clock.fixed(Instant.parse("2026-07-08T10:00:00Z"), ZoneOffset.UTC);

  private static final String SUB_OWNER = "user-1";
  private static final String SUB_OTHER = "user-2";
  private static final String DISPLAY = "Manne";

  private static KanbanAccessToken token(long id, String userSub) {
    return new KanbanAccessToken(
        id, userSub, DISPLAY, "Board", "hashed", Instant.EPOCH, null, false);
  }

  // ----- create -------------------------------------------------------------

  @Test
  void createReturnsPlaintextAndPersistsHash() {
    given(crypto.generatePlaintext()).willReturn("tk_secret");
    given(crypto.hash("tk_secret")).willReturn("hashed");
    given(tokens.save(any())).willAnswer(inv -> withId((KanbanAccessToken) inv.getArgument(0), 7L));

    final CreatedKanbanToken created =
        new CreateKanbanTokenUseCase(tokens, crypto).execute(SUB_OWNER, DISPLAY, "Board");

    assertThat(created.plaintext()).isEqualTo("tk_secret");
    assertThat(created.token().tokenHash()).isEqualTo("hashed");
    assertThat(created.token().id()).isEqualTo(7L);
    assertThat(created.token().displayName()).isEqualTo(DISPLAY);
  }

  // ----- list ---------------------------------------------------------------

  @Test
  void listReturnsAllOfUser() {
    given(tokens.findAllByUser(SUB_OWNER)).willReturn(List.of(token(1L, SUB_OWNER)));

    final List<KanbanAccessToken> result = new ListKanbanTokensUseCase(tokens).execute(SUB_OWNER);

    assertThat(result).hasSize(1);
  }

  // ----- revoke -------------------------------------------------------------

  @Test
  void revokePersistsRevokedFlag() {
    given(tokens.findById(1L)).willReturn(Optional.of(token(1L, SUB_OWNER)));

    new RevokeKanbanTokenUseCase(tokens).execute(SUB_OWNER, 1L);

    verify(tokens).save(any());
  }

  @Test
  void revokeIsIdempotentWhenAlreadyRevoked() {
    final KanbanAccessToken alreadyRevoked = token(1L, SUB_OWNER).withRevoked();
    given(tokens.findById(1L)).willReturn(Optional.of(alreadyRevoked));

    new RevokeKanbanTokenUseCase(tokens).execute(SUB_OWNER, 1L);

    verify(tokens, never()).save(any());
  }

  @Test
  void revokeThrowsForForeignToken() {
    given(tokens.findById(1L)).willReturn(Optional.of(token(1L, SUB_OWNER)));

    assertThatThrownBy(() -> new RevokeKanbanTokenUseCase(tokens).execute(SUB_OTHER, 1L))
        .isInstanceOf(KanbanTokenNotFoundException.class);
    verify(tokens, never()).save(any());
  }

  @Test
  void revokeThrowsWhenMissing() {
    given(tokens.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> new RevokeKanbanTokenUseCase(tokens).execute(SUB_OWNER, 99L))
        .isInstanceOf(KanbanTokenNotFoundException.class);
  }

  // ----- resolve ------------------------------------------------------------

  @Test
  void resolveLooksUpAndUpdatesLastUsed() {
    given(crypto.hash("tk_secret")).willReturn("hashed");
    given(tokens.findActiveByHash("hashed")).willReturn(Optional.of(token(1L, SUB_OWNER)));
    given(tokens.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanAccessToken result =
        new ResolveKanbanTokenUseCase(tokens, crypto, fixed).execute("tk_secret");

    assertThat(result.lastUsedAt()).isEqualTo(Instant.parse("2026-07-08T10:00:00Z"));
    assertThat(result.userSub()).isEqualTo(SUB_OWNER);
    assertThat(result.displayName()).isEqualTo(DISPLAY);
    verify(tokens).save(any());
  }

  @Test
  void resolveThrowsForNullPlaintext() {
    assertThatThrownBy(() -> new ResolveKanbanTokenUseCase(tokens, crypto, fixed).execute(null))
        .isInstanceOf(InvalidKanbanTokenException.class);
  }

  @Test
  void resolveThrowsForBlankPlaintext() {
    assertThatThrownBy(() -> new ResolveKanbanTokenUseCase(tokens, crypto, fixed).execute("   "))
        .isInstanceOf(InvalidKanbanTokenException.class);
  }

  @Test
  void resolveThrowsWhenHashUnknown() {
    given(crypto.hash("tk_secret")).willReturn("hashed");
    given(tokens.findActiveByHash("hashed")).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> new ResolveKanbanTokenUseCase(tokens, crypto, fixed).execute("tk_secret"))
        .isInstanceOf(InvalidKanbanTokenException.class);
  }

  // ----- helpers ------------------------------------------------------------

  private static KanbanAccessToken withId(KanbanAccessToken token, long id) {
    return new KanbanAccessToken(
        id,
        token.userSub(),
        token.displayName(),
        token.name(),
        token.tokenHash(),
        Instant.EPOCH,
        token.lastUsedAt(),
        token.revoked());
  }
}
