package org.mwolff.api.ingest.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.ingest.domain.IngestToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaIngestTokenAdapter.class)
class JpaIngestTokenAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaIngestTokenAdapter adapter;

  private static final String USER_A = "user-a";
  private static final String USER_B = "user-b";

  @Test
  void persistsAndReadsToken() {
    final IngestToken saved = adapter.save(IngestToken.newInstance(USER_A, "Pi", "hash-a"));

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(adapter.findAllByUser(USER_A)).hasSize(1);
  }

  @Test
  void filtersByUser() {
    adapter.save(IngestToken.newInstance(USER_A, "A", "hash-1"));
    adapter.save(IngestToken.newInstance(USER_B, "B", "hash-2"));

    assertThat(adapter.findAllByUser(USER_A)).extracting(IngestToken::name).containsExactly("A");
    assertThat(adapter.findAllByUser(USER_B)).extracting(IngestToken::name).containsExactly("B");
  }

  @Test
  void findsActiveByHashOnly() {
    final IngestToken active = adapter.save(IngestToken.newInstance(USER_A, "A", "hash-active"));
    final IngestToken inactive = adapter.save(IngestToken.newInstance(USER_A, "B", "hash-revoked"));
    adapter.save(inactive.withRevoked());

    assertThat(adapter.findActiveByHash("hash-active"))
        .hasValueSatisfying(t -> assertThat(t.id()).isEqualTo(active.id()));
    assertThat(adapter.findActiveByHash("hash-revoked")).isEmpty();
  }

  @Test
  void updatesRevokedFlagOnExistingToken() {
    final IngestToken original = adapter.save(IngestToken.newInstance(USER_A, "Pi", "hash-x"));

    final IngestToken revoked = adapter.save(original.withRevoked());

    assertThat(revoked.id()).isEqualTo(original.id());
    assertThat(revoked.revoked()).isTrue();
  }

  @Test
  void updatesLastUsedAtOnExistingToken() {
    final IngestToken original = adapter.save(IngestToken.newInstance(USER_A, "Pi", "hash-y"));
    final Instant when = Instant.parse("2026-05-27T12:00:00Z");

    final IngestToken used = adapter.save(original.withLastUsedAt(when));

    assertThat(used.lastUsedAt()).isEqualTo(when);
  }
}
