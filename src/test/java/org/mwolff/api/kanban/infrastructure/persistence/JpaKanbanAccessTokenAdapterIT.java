package org.mwolff.api.kanban.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.kanban.domain.KanbanAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaKanbanAccessTokenAdapter.class)
class JpaKanbanAccessTokenAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaKanbanAccessTokenAdapter adapter;

  private static final String USER_A = "user-a";
  private static final String USER_B = "user-b";

  @Test
  void persistsAndReadsToken() {
    final KanbanAccessToken saved =
        adapter.save(KanbanAccessToken.newInstance(USER_A, "Manne", "Board", "hash-a"));

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(saved.displayName()).isEqualTo("Manne");
    assertThat(adapter.findAllByUser(USER_A)).hasSize(1);
  }

  @Test
  void filtersByUser() {
    adapter.save(KanbanAccessToken.newInstance(USER_A, "Manne", "A", "hash-1"));
    adapter.save(KanbanAccessToken.newInstance(USER_B, "Gabi", "B", "hash-2"));

    assertThat(adapter.findAllByUser(USER_A))
        .extracting(KanbanAccessToken::name)
        .containsExactly("A");
    assertThat(adapter.findAllByUser(USER_B))
        .extracting(KanbanAccessToken::name)
        .containsExactly("B");
  }

  @Test
  void findsActiveByHashOnly() {
    final KanbanAccessToken active =
        adapter.save(KanbanAccessToken.newInstance(USER_A, "Manne", "A", "hash-active"));
    final KanbanAccessToken inactive =
        adapter.save(KanbanAccessToken.newInstance(USER_A, "Manne", "B", "hash-revoked"));
    adapter.save(inactive.withRevoked());

    assertThat(adapter.findActiveByHash("hash-active"))
        .hasValueSatisfying(
            t -> {
              assertThat(t.id()).isEqualTo(active.id());
              assertThat(t.displayName()).isEqualTo("Manne");
            });
    assertThat(adapter.findActiveByHash("hash-revoked")).isEmpty();
  }

  @Test
  void updatesRevokedFlagOnExistingToken() {
    final KanbanAccessToken original =
        adapter.save(KanbanAccessToken.newInstance(USER_A, "Manne", "Board", "hash-x"));

    final KanbanAccessToken revoked = adapter.save(original.withRevoked());

    assertThat(revoked.id()).isEqualTo(original.id());
    assertThat(revoked.revoked()).isTrue();
  }

  @Test
  void updatesLastUsedAtOnExistingToken() {
    final KanbanAccessToken original =
        adapter.save(KanbanAccessToken.newInstance(USER_A, "Manne", "Board", "hash-y"));
    final Instant when = Instant.parse("2026-07-08T12:00:00Z");

    final KanbanAccessToken used = adapter.save(original.withLastUsedAt(when));

    assertThat(used.lastUsedAt()).isEqualTo(when);
  }
}
