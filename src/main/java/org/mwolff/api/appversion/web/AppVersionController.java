package org.mwolff.api.appversion.web;

import org.mwolff.api.appversion.application.GetAppVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMajorVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMinorVersionUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Adapter fuer die Anwendungsversion. Oeffentlich erreichbar (kein Auth-Gate fuer MVP — faellt
 * unter {@code anyRequest().permitAll()} der SecurityConfig).
 */
@RestController
@RequestMapping("/api/app/version")
public class AppVersionController {

  private final GetAppVersionUseCase getUseCase;
  private final IncrementMinorVersionUseCase incrementMinorUseCase;
  private final IncrementMajorVersionUseCase incrementMajorUseCase;

  public AppVersionController(
      final GetAppVersionUseCase getUseCase,
      final IncrementMinorVersionUseCase incrementMinorUseCase,
      final IncrementMajorVersionUseCase incrementMajorUseCase) {
    this.getUseCase = getUseCase;
    this.incrementMinorUseCase = incrementMinorUseCase;
    this.incrementMajorUseCase = incrementMajorUseCase;
  }

  @GetMapping
  public AppVersionResponse get() {
    return AppVersionResponse.from(getUseCase.execute());
  }

  @PostMapping("/increment-minor")
  public AppVersionResponse incrementMinor() {
    return AppVersionResponse.from(incrementMinorUseCase.execute());
  }

  @PostMapping("/increment-major")
  public AppVersionResponse incrementMajor() {
    return AppVersionResponse.from(incrementMajorUseCase.execute());
  }
}
