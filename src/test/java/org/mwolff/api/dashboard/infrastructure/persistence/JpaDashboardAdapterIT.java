package org.mwolff.api.dashboard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mwolff.api.AbstractIntegrationTest;
import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPosition;
import org.mwolff.api.dashboard.domain.WidgetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Integrationstest des JPA-Adapters gegen eine echte MariaDB via Testcontainers. Prüft, dass
 * Domain-Records korrekt persistiert werden, dass Owner-Filterung und Default-Reset funktionieren
 * und dass Widget-Replace transactional ist.
 *
 * <p>Läuft als {@code *IT} via Failsafe und braucht einen Docker-Daemon.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaDashboardAdapter.class)
class JpaDashboardAdapterIT extends AbstractIntegrationTest {

  @Autowired private JpaDashboardAdapter adapter;

  private static final String USER_A = "user-a";
  private static final String USER_B = "user-b";

  @Test
  void shouldPersistAndReadDashboard() {
    final Dashboard saved = adapter.save(Dashboard.newInstance(USER_A, "Main", true));

    assertThat(saved.id()).isNotNull();
    assertThat(saved.createdAt()).isNotNull();
    assertThat(saved.updatedAt()).isNotNull();

    final List<Dashboard> all = adapter.findAllByUser(USER_A);
    assertThat(all).hasSize(1);
    assertThat(all.get(0).name()).isEqualTo("Main");
    assertThat(all.get(0).isDefault()).isTrue();
  }

  @Test
  void shouldFilterByUser() {
    adapter.save(Dashboard.newInstance(USER_A, "Owned by A", true));
    adapter.save(Dashboard.newInstance(USER_B, "Owned by B", true));

    assertThat(adapter.findAllByUser(USER_A))
        .extracting(Dashboard::name)
        .containsExactly("Owned by A");
    assertThat(adapter.findAllByUser(USER_B))
        .extracting(Dashboard::name)
        .containsExactly("Owned by B");
  }

  @Test
  void shouldClearDefaultForUserOnly() {
    adapter.save(Dashboard.newInstance(USER_A, "A-1", true));
    adapter.save(Dashboard.newInstance(USER_B, "B-1", true));

    adapter.clearDefaultForUser(USER_A);

    assertThat(adapter.findDefaultByUser(USER_A)).isEmpty();
    assertThat(adapter.findDefaultByUser(USER_B)).isPresent();
  }

  @Test
  void shouldFindDefaultByUser() {
    adapter.save(Dashboard.newInstance(USER_A, "A-1", false));
    final Dashboard def = adapter.save(Dashboard.newInstance(USER_A, "A-default", true));

    assertThat(adapter.findDefaultByUser(USER_A))
        .hasValueSatisfying(
            d -> {
              assertThat(d.id()).isEqualTo(def.id());
              assertThat(d.name()).isEqualTo("A-default");
            });
  }

  @Test
  void shouldFindByIdRegardlessOfOwner() {
    final Dashboard a = adapter.save(Dashboard.newInstance(USER_A, "owned", true));
    // findById ist owner-agnostisch — Owner-Check ist Sache der Use-Cases.
    assertThat(adapter.findById(a.id())).isPresent();
  }

  @Test
  void shouldDeleteById() {
    final Dashboard d = adapter.save(Dashboard.newInstance(USER_A, "to delete", true));

    adapter.deleteById(d.id());

    assertThat(adapter.findById(d.id())).isEmpty();
  }

  @Test
  void shouldReplaceAllWidgetsForDashboard() {
    final Dashboard d = adapter.save(Dashboard.newInstance(USER_A, "with-widgets", true));
    final Widget first =
        Widget.newInstance(d.id(), WidgetType.TEXTBOX, new WidgetPosition(0, 0, 2, 2), "{\"v\":1}");
    final Widget second =
        Widget.newInstance(d.id(), WidgetType.KPI, new WidgetPosition(2, 0, 2, 2), "{\"v\":2}");

    final List<Widget> saved = adapter.replaceAllForDashboard(d.id(), List.of(first, second));
    assertThat(saved).hasSize(2);

    // Replace with single widget — old two must be gone.
    final Widget replacement =
        Widget.newInstance(d.id(), WidgetType.KPI, new WidgetPosition(0, 0, 4, 4), "{\"v\":3}");
    final List<Widget> after = adapter.replaceAllForDashboard(d.id(), List.of(replacement));

    assertThat(after).hasSize(1);
    assertThat(adapter.findAllByDashboard(d.id())).hasSize(1);
  }

  @Test
  void shouldDeleteWidgetsByDashboard() {
    final Dashboard d = adapter.save(Dashboard.newInstance(USER_A, "cleanup", true));
    final Widget w =
        Widget.newInstance(d.id(), WidgetType.TEXTBOX, new WidgetPosition(0, 0, 1, 1), "{}");
    adapter.replaceAllForDashboard(d.id(), List.of(w));

    adapter.deleteByDashboard(d.id());

    assertThat(adapter.findAllByDashboard(d.id())).isEmpty();
  }

  @Test
  void shouldUpdateDefaultFlagOnExistingDashboard() {
    final Dashboard original = adapter.save(Dashboard.newInstance(USER_A, "main", false));
    final Dashboard updated = adapter.save(original.withDefault(true));

    assertThat(updated.id()).isEqualTo(original.id());
    assertThat(updated.isDefault()).isTrue();
  }

  // Regression #104: save() musste den Namen-Update durchreichen. Vor dem Fix wurde nur
  // setDefault() auf der gemanagten Entity aufgerufen, der Name blieb in der DB unverändert.
  @Test
  void shouldUpdateNameOnExistingDashboard() {
    final Dashboard original = adapter.save(Dashboard.newInstance(USER_A, "Neues Dashboard", true));
    final Dashboard renamed = adapter.save(original.withName("Mein Dashboard"));

    assertThat(renamed.id()).isEqualTo(original.id());
    assertThat(renamed.name()).isEqualTo("Mein Dashboard");
    // Reload aus dem Repo, um sicherzustellen, dass die Änderung wirklich persistiert ist und
    // nicht nur im Domain-Rückgabewert steht.
    assertThat(adapter.findById(original.id()))
        .hasValueSatisfying(d -> assertThat(d.name()).isEqualTo("Mein Dashboard"));
  }
}
