package org.mwolff.api.kanban.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class KanbanAccessTokenTest {

  private static final String USER = "user-1";
  private static final String DISPLAY = "Manne";

  @Test
  void newInstanceShouldStartActive() {
    final KanbanAccessToken token = KanbanAccessToken.newInstance(USER, DISPLAY, "Board", "hash");

    assertThat(token.id()).isNull();
    assertThat(token.userSub()).isEqualTo(USER);
    assertThat(token.displayName()).isEqualTo(DISPLAY);
    assertThat(token.name()).isEqualTo("Board");
    assertThat(token.tokenHash()).isEqualTo("hash");
    assertThat(token.revoked()).isFalse();
    assertThat(token.lastUsedAt()).isNull();
  }

  @Test
  void withRevokedShouldSetFlag() {
    final KanbanAccessToken token = KanbanAccessToken.newInstance(USER, DISPLAY, "Board", "hash");

    final KanbanAccessToken revoked = token.withRevoked();

    assertThat(revoked.revoked()).isTrue();
    assertThat(revoked.tokenHash()).isEqualTo(token.tokenHash());
    assertThat(revoked.displayName()).isEqualTo(DISPLAY);
  }

  @Test
  void withLastUsedAtShouldUpdateTimestamp() {
    final KanbanAccessToken token = KanbanAccessToken.newInstance(USER, DISPLAY, "Board", "hash");
    final Instant now = Instant.parse("2026-07-08T10:00:00Z");

    final KanbanAccessToken updated = token.withLastUsedAt(now);

    assertThat(updated.lastUsedAt()).isEqualTo(now);
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(() -> new KanbanAccessToken(null, " ", DISPLAY, "B", "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(
            () -> new KanbanAccessToken(null, null, DISPLAY, "B", "h", null, null, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankDisplayName() {
    assertThatThrownBy(() -> new KanbanAccessToken(null, "u", " ", "B", "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullDisplayName() {
    assertThatThrownBy(() -> new KanbanAccessToken(null, "u", null, "B", "h", null, null, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectDisplayNameTooLong() {
    final String tooLong = "x".repeat(KanbanAccessToken.MAX_DISPLAY_NAME_LENGTH + 1);
    assertThatThrownBy(() -> new KanbanAccessToken(null, "u", tooLong, "B", "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(() -> new KanbanAccessToken(null, "u", DISPLAY, " ", "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullName() {
    assertThatThrownBy(
            () -> new KanbanAccessToken(null, "u", DISPLAY, null, "h", null, null, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectNameTooLong() {
    final String tooLong = "x".repeat(KanbanAccessToken.MAX_NAME_LENGTH + 1);
    assertThatThrownBy(
            () -> new KanbanAccessToken(null, "u", DISPLAY, tooLong, "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankHash() {
    assertThatThrownBy(() -> new KanbanAccessToken(null, "u", DISPLAY, "B", " ", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullHash() {
    assertThatThrownBy(
            () -> new KanbanAccessToken(null, "u", DISPLAY, "B", null, null, null, false))
        .isInstanceOf(NullPointerException.class);
  }
}
