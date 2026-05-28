package org.mwolff.api.ingest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class IngestTokenTest {

  private static final String USER = "user-1";

  @Test
  void newInstanceShouldStartActive() {
    final IngestToken token = IngestToken.newInstance(USER, "Pi", "hash");

    assertThat(token.id()).isNull();
    assertThat(token.userSub()).isEqualTo(USER);
    assertThat(token.name()).isEqualTo("Pi");
    assertThat(token.tokenHash()).isEqualTo("hash");
    assertThat(token.revoked()).isFalse();
    assertThat(token.lastUsedAt()).isNull();
  }

  @Test
  void withRevokedShouldSetFlag() {
    final IngestToken token = IngestToken.newInstance(USER, "Pi", "hash");

    final IngestToken revoked = token.withRevoked();

    assertThat(revoked.revoked()).isTrue();
    assertThat(revoked.tokenHash()).isEqualTo(token.tokenHash());
  }

  @Test
  void withLastUsedAtShouldUpdateTimestamp() {
    final IngestToken token = IngestToken.newInstance(USER, "Pi", "hash");
    final Instant now = Instant.parse("2026-05-28T10:00:00Z");

    final IngestToken updated = token.withLastUsedAt(now);

    assertThat(updated.lastUsedAt()).isEqualTo(now);
  }

  @Test
  void shouldRejectBlankUserSub() {
    assertThatThrownBy(() -> new IngestToken(null, " ", "Pi", "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullUserSub() {
    assertThatThrownBy(() -> new IngestToken(null, null, "Pi", "h", null, null, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(() -> new IngestToken(null, "u", " ", "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullName() {
    assertThatThrownBy(() -> new IngestToken(null, "u", null, "h", null, null, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectNameTooLong() {
    final String tooLong = "x".repeat(IngestToken.MAX_NAME_LENGTH + 1);
    assertThatThrownBy(() -> new IngestToken(null, "u", tooLong, "h", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankHash() {
    assertThatThrownBy(() -> new IngestToken(null, "u", "Pi", " ", null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullHash() {
    assertThatThrownBy(() -> new IngestToken(null, "u", "Pi", null, null, null, false))
        .isInstanceOf(NullPointerException.class);
  }
}
