package org.mwolff.api.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.dashboard.application.GetDashboardUseCase.DashboardWithWidgets;
import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.DashboardNotFoundException;
import org.mwolff.api.dashboard.domain.DashboardPort;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.domain.WidgetPort;
import org.mwolff.api.dashboard.domain.WidgetPosition;
import org.mwolff.api.dashboard.domain.WidgetType;

class DashboardUseCasesTest {

  private final DashboardPort dashboards = mock(DashboardPort.class);
  private final WidgetPort widgets = mock(WidgetPort.class);

  private static final String SUB_OWNER = "user-1";
  private static final String SUB_OTHER = "user-2";

  private static Dashboard dashboard(long id, String userSub, boolean isDefault) {
    return new Dashboard(id, userSub, "Main", isDefault, Instant.EPOCH, Instant.EPOCH);
  }

  // ----- list ---------------------------------------------------------------

  @Test
  void listShouldReturnDashboardsForOwner() {
    given(dashboards.findAllByUser(SUB_OWNER))
        .willReturn(List.of(dashboard(1L, SUB_OWNER, true), dashboard(2L, SUB_OWNER, false)));

    final List<Dashboard> result = new ListDashboardsUseCase(dashboards).execute(SUB_OWNER);

    assertThat(result).hasSize(2);
  }

  // ----- create -------------------------------------------------------------

  @Test
  void createShouldMarkFirstDashboardAsDefault() {
    given(dashboards.findAllByUser(SUB_OWNER)).willReturn(List.of());
    given(dashboards.save(any()))
        .willAnswer(invocation -> withId((Dashboard) invocation.getArgument(0), 1L));

    final Dashboard created = new CreateDashboardUseCase(dashboards).execute(SUB_OWNER, "Main");

    assertThat(created.isDefault()).isTrue();
  }

  @Test
  void createShouldNotMarkSubsequentDashboardsAsDefault() {
    given(dashboards.findAllByUser(SUB_OWNER)).willReturn(List.of(dashboard(1L, SUB_OWNER, true)));
    given(dashboards.save(any()))
        .willAnswer(invocation -> withId((Dashboard) invocation.getArgument(0), 2L));

    final Dashboard created =
        new CreateDashboardUseCase(dashboards).execute(SUB_OWNER, "Side panel");

    assertThat(created.isDefault()).isFalse();
  }

  // ----- get ----------------------------------------------------------------

  @Test
  void getShouldReturnDashboardAndWidgetsForOwner() {
    given(dashboards.findById(1L)).willReturn(Optional.of(dashboard(1L, SUB_OWNER, true)));
    given(widgets.findAllByDashboard(1L)).willReturn(List.of(widget(10L, 1L, WidgetType.TEXTBOX)));

    final DashboardWithWidgets result =
        new GetDashboardUseCase(dashboards, widgets).execute(SUB_OWNER, 1L);

    assertThat(result.dashboard().id()).isEqualTo(1L);
    assertThat(result.widgets()).hasSize(1);
  }

  @Test
  void getShouldThrowNotFoundForForeignDashboard() {
    given(dashboards.findById(1L)).willReturn(Optional.of(dashboard(1L, SUB_OWNER, true)));

    assertThatThrownBy(() -> new GetDashboardUseCase(dashboards, widgets).execute(SUB_OTHER, 1L))
        .isInstanceOf(DashboardNotFoundException.class);
  }

  @Test
  void getShouldThrowNotFoundWhenDashboardMissing() {
    given(dashboards.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> new GetDashboardUseCase(dashboards, widgets).execute(SUB_OWNER, 99L))
        .isInstanceOf(DashboardNotFoundException.class);
  }

  // ----- updateLayout -------------------------------------------------------

  @Test
  void updateLayoutShouldReplaceWidgetsForOwner() {
    given(dashboards.findById(1L)).willReturn(Optional.of(dashboard(1L, SUB_OWNER, true)));
    final List<Widget> newWidgets = List.of(widget(null, 1L, WidgetType.KPI));
    given(widgets.replaceAllForDashboard(1L, newWidgets)).willReturn(newWidgets);

    final List<Widget> result =
        new UpdateLayoutUseCase(dashboards, widgets).execute(SUB_OWNER, 1L, newWidgets);

    assertThat(result).hasSize(1);
    verify(widgets).replaceAllForDashboard(1L, newWidgets);
  }

  @Test
  void updateLayoutShouldThrowNotFoundForForeignDashboard() {
    given(dashboards.findById(1L)).willReturn(Optional.of(dashboard(1L, SUB_OWNER, true)));

    assertThatThrownBy(
            () -> new UpdateLayoutUseCase(dashboards, widgets).execute(SUB_OTHER, 1L, List.of()))
        .isInstanceOf(DashboardNotFoundException.class);
    verify(widgets, never()).replaceAllForDashboard(anyLong(), any());
  }

  @Test
  void updateLayoutShouldThrowNotFoundWhenDashboardMissing() {
    given(dashboards.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> new UpdateLayoutUseCase(dashboards, widgets).execute(SUB_OWNER, 99L, List.of()))
        .isInstanceOf(DashboardNotFoundException.class);
  }

  // ----- markAsDefault ------------------------------------------------------

  @Test
  void markAsDefaultShouldClearOldDefaultsBeforeSettingNew() {
    final Dashboard existing = dashboard(2L, SUB_OWNER, false);
    given(dashboards.findById(2L)).willReturn(Optional.of(existing));
    given(dashboards.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    final Dashboard result = new MarkAsDefaultUseCase(dashboards).execute(SUB_OWNER, 2L);

    assertThat(result.isDefault()).isTrue();
    verify(dashboards).clearDefaultForUser(SUB_OWNER);
    verify(dashboards).save(any());
  }

  @Test
  void markAsDefaultShouldThrowForForeignDashboard() {
    given(dashboards.findById(2L)).willReturn(Optional.of(dashboard(2L, SUB_OWNER, false)));

    assertThatThrownBy(() -> new MarkAsDefaultUseCase(dashboards).execute(SUB_OTHER, 2L))
        .isInstanceOf(DashboardNotFoundException.class);
    verify(dashboards, never()).clearDefaultForUser(any());
  }

  @Test
  void markAsDefaultShouldThrowWhenDashboardMissing() {
    given(dashboards.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> new MarkAsDefaultUseCase(dashboards).execute(SUB_OWNER, 99L))
        .isInstanceOf(DashboardNotFoundException.class);
  }

  // ----- delete -------------------------------------------------------------

  @Test
  void deleteShouldRemoveWidgetsAndDashboardForOwner() {
    given(dashboards.findById(1L)).willReturn(Optional.of(dashboard(1L, SUB_OWNER, true)));

    new DeleteDashboardUseCase(dashboards, widgets).execute(SUB_OWNER, 1L);

    verify(widgets).deleteByDashboard(1L);
    verify(dashboards).deleteById(1L);
  }

  @Test
  void deleteShouldThrowForForeignDashboard() {
    given(dashboards.findById(1L)).willReturn(Optional.of(dashboard(1L, SUB_OWNER, true)));

    assertThatThrownBy(() -> new DeleteDashboardUseCase(dashboards, widgets).execute(SUB_OTHER, 1L))
        .isInstanceOf(DashboardNotFoundException.class);
    verify(widgets, never()).deleteByDashboard(anyLong());
    verify(dashboards, never()).deleteById(anyLong());
  }

  @Test
  void deleteShouldThrowWhenDashboardMissing() {
    given(dashboards.findById(99L)).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> new DeleteDashboardUseCase(dashboards, widgets).execute(SUB_OWNER, 99L))
        .isInstanceOf(DashboardNotFoundException.class);
  }

  // ----- getDefault ---------------------------------------------------------

  @Test
  void getDefaultShouldReturnMarkedDefault() {
    given(dashboards.findDefaultByUser(SUB_OWNER))
        .willReturn(Optional.of(dashboard(1L, SUB_OWNER, true)));

    final Optional<Dashboard> result =
        new GetDefaultDashboardUseCase(dashboards).execute(SUB_OWNER);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(1L);
  }

  @Test
  void getDefaultShouldFallBackToFirstWhenNoneMarked() {
    given(dashboards.findDefaultByUser(SUB_OWNER)).willReturn(Optional.empty());
    given(dashboards.findAllByUser(SUB_OWNER))
        .willReturn(List.of(dashboard(7L, SUB_OWNER, false), dashboard(8L, SUB_OWNER, false)));

    final Optional<Dashboard> result =
        new GetDefaultDashboardUseCase(dashboards).execute(SUB_OWNER);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(7L);
  }

  @Test
  void getDefaultShouldReturnEmptyWhenUserHasNoDashboards() {
    given(dashboards.findDefaultByUser(SUB_OWNER)).willReturn(Optional.empty());
    given(dashboards.findAllByUser(SUB_OWNER)).willReturn(List.of());

    final Optional<Dashboard> result =
        new GetDefaultDashboardUseCase(dashboards).execute(SUB_OWNER);

    assertThat(result).isEmpty();
  }

  // ----- helpers ------------------------------------------------------------

  private static Widget widget(Long id, Long dashboardId, WidgetType type) {
    return new Widget(
        id, dashboardId, type, new WidgetPosition(0, 0, 2, 2), "{}", Instant.EPOCH, Instant.EPOCH);
  }

  private static Dashboard withId(Dashboard d, long id) {
    return new Dashboard(id, d.userSub(), d.name(), d.isDefault(), Instant.EPOCH, Instant.EPOCH);
  }
}
