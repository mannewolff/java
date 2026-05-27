package org.mwolff.api.dashboard.web;

import java.util.List;

import jakarta.validation.Valid;

import org.mwolff.api.dashboard.application.CreateDashboardUseCase;
import org.mwolff.api.dashboard.application.DeleteDashboardUseCase;
import org.mwolff.api.dashboard.application.GetDashboardUseCase;
import org.mwolff.api.dashboard.application.ListDashboardsUseCase;
import org.mwolff.api.dashboard.application.MarkAsDefaultUseCase;
import org.mwolff.api.dashboard.application.RenameDashboardUseCase;
import org.mwolff.api.dashboard.application.UpdateLayoutUseCase;
import org.mwolff.api.dashboard.domain.Dashboard;
import org.mwolff.api.dashboard.domain.Widget;
import org.mwolff.api.dashboard.web.dto.CreateDashboardRequest;
import org.mwolff.api.dashboard.web.dto.DashboardDetailResponse;
import org.mwolff.api.dashboard.web.dto.DashboardSummaryResponse;
import org.mwolff.api.dashboard.web.dto.RenameDashboardRequest;
import org.mwolff.api.dashboard.web.dto.UpdateLayoutRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter für Dashboards. Alle Endpoints sind durch {@code
 * SecurityConfig#requestMatchers("/api/dashboards/**").hasRole("USER")} geschützt. Owner-Check
 * passiert in den Use-Cases — der Controller leitet nur {@code sub} aus dem JWT weiter.
 */
@RestController
@RequestMapping("/api/dashboards")
public class DashboardController {

  private final ListDashboardsUseCase listUseCase;
  private final CreateDashboardUseCase createUseCase;
  private final GetDashboardUseCase getUseCase;
  private final UpdateLayoutUseCase updateLayoutUseCase;
  private final MarkAsDefaultUseCase markDefaultUseCase;
  private final RenameDashboardUseCase renameUseCase;
  private final DeleteDashboardUseCase deleteUseCase;

  public DashboardController(
      ListDashboardsUseCase listUseCase,
      CreateDashboardUseCase createUseCase,
      GetDashboardUseCase getUseCase,
      UpdateLayoutUseCase updateLayoutUseCase,
      MarkAsDefaultUseCase markDefaultUseCase,
      RenameDashboardUseCase renameUseCase,
      DeleteDashboardUseCase deleteUseCase) {
    this.listUseCase = listUseCase;
    this.createUseCase = createUseCase;
    this.getUseCase = getUseCase;
    this.updateLayoutUseCase = updateLayoutUseCase;
    this.markDefaultUseCase = markDefaultUseCase;
    this.renameUseCase = renameUseCase;
    this.deleteUseCase = deleteUseCase;
  }

  @GetMapping
  public List<DashboardSummaryResponse> list(JwtAuthenticationToken auth) {
    return listUseCase.execute(auth.getToken().getSubject()).stream()
        .map(DashboardSummaryResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<DashboardSummaryResponse> create(
      JwtAuthenticationToken auth, @Valid @RequestBody CreateDashboardRequest body) {
    final Dashboard created = createUseCase.execute(auth.getToken().getSubject(), body.name());
    return ResponseEntity.status(HttpStatus.CREATED).body(DashboardSummaryResponse.from(created));
  }

  @GetMapping("/{id}")
  public DashboardDetailResponse get(JwtAuthenticationToken auth, @PathVariable long id) {
    return DashboardDetailResponse.from(getUseCase.execute(auth.getToken().getSubject(), id));
  }

  @PutMapping("/{id}")
  public DashboardDetailResponse updateLayout(
      JwtAuthenticationToken auth,
      @PathVariable long id,
      @Valid @RequestBody UpdateLayoutRequest body) {
    final String sub = auth.getToken().getSubject();
    final List<Widget> domainWidgets =
        body.widgets().stream().map(dto -> dto.toDomain(id)).toList();
    updateLayoutUseCase.execute(sub, id, domainWidgets);
    // Nach dem Layout-Replace komplette Dashboard-Sicht zurueckgeben — konsistent zu GET.
    return DashboardDetailResponse.from(getUseCase.execute(sub, id));
  }

  @PutMapping("/{id}/default")
  public DashboardSummaryResponse markAsDefault(
      JwtAuthenticationToken auth, @PathVariable long id) {
    return DashboardSummaryResponse.from(
        markDefaultUseCase.execute(auth.getToken().getSubject(), id));
  }

  @PutMapping("/{id}/name")
  public DashboardSummaryResponse rename(
      JwtAuthenticationToken auth,
      @PathVariable long id,
      @Valid @RequestBody RenameDashboardRequest body) {
    return DashboardSummaryResponse.from(
        renameUseCase.execute(auth.getToken().getSubject(), id, body.name()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(JwtAuthenticationToken auth, @PathVariable long id) {
    deleteUseCase.execute(auth.getToken().getSubject(), id);
    return ResponseEntity.noContent().build();
  }
}
