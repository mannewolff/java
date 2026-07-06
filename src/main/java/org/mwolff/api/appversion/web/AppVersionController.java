package org.mwolff.api.appversion.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.mwolff.api.appversion.application.GetAppVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMajorVersionUseCase;
import org.mwolff.api.appversion.application.IncrementMinorVersionUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST-Adapter fuer die Anwendungsversion.
 *
 * <p>{@code GET} ist per SecurityConfig auf {@code ROLE_USER} beschraenkt (Anzeige im Header). Die
 * mutierenden {@code POST}-Endpunkte sind auf Security-Ebene {@code permitAll}, werden hier aber
 * per Shared-Secret-Header ({@value #TOKEN_HEADER}) geschuetzt (#229): Das Deploy-Skript (#225)
 * ruft sie server-zu-server ohne JWT auf, deshalb kein rollenbasierter Gate. Fehlt das Secret in
 * der Konfiguration oder passt der Header nicht, antworten die POSTs mit 401.
 */
@RestController
@RequestMapping("/api/app/version")
public class AppVersionController {

  static final String TOKEN_HEADER = "X-Version-Token";

  /** Gemeinsamer Rate-Limit-Schlüssel für beide Increment-Endpunkte (globales Fenster, #311). */
  private static final String RATE_LIMIT_KEY = "app-version-increment";

  private final GetAppVersionUseCase getUseCase;
  private final IncrementMinorVersionUseCase incrementMinorUseCase;
  private final IncrementMajorVersionUseCase incrementMajorUseCase;
  private final AppVersionRateLimiter rateLimiter;
  private final String incrementSecret;

  public AppVersionController(
      final GetAppVersionUseCase getUseCase,
      final IncrementMinorVersionUseCase incrementMinorUseCase,
      final IncrementMajorVersionUseCase incrementMajorUseCase,
      final AppVersionRateLimiter rateLimiter,
      @Value("${app.version.increment-secret:}") final String incrementSecret) {
    this.getUseCase = getUseCase;
    this.incrementMinorUseCase = incrementMinorUseCase;
    this.incrementMajorUseCase = incrementMajorUseCase;
    this.rateLimiter = rateLimiter;
    this.incrementSecret = incrementSecret;
  }

  @GetMapping
  public AppVersionResponse get() {
    return AppVersionResponse.from(getUseCase.execute());
  }

  @PostMapping("/increment-minor")
  public AppVersionResponse incrementMinor(
      @RequestHeader(value = TOKEN_HEADER, required = false) final String token) {
    requireValidToken(token);
    return AppVersionResponse.from(incrementMinorUseCase.execute());
  }

  @PostMapping("/increment-major")
  public AppVersionResponse incrementMajor(
      @RequestHeader(value = TOKEN_HEADER, required = false) final String token) {
    requireValidToken(token);
    return AppVersionResponse.from(incrementMajorUseCase.execute());
  }

  private void requireValidToken(final String token) {
    // Drosselung VOR der Secret-Prüfung, damit jeder Brute-Force-Versuch aufs Limit zählt (#311).
    if (!rateLimiter.tryAcquire(RATE_LIMIT_KEY)) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many version-increment attempts.");
    }
    if (!isAuthorized(token)) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Invalid or missing version token.");
    }
  }

  /**
   * Konstantzeit-Vergleich des Headers gegen das konfigurierte Secret. Ist kein Secret gesetzt
   * (leer), wird grundsaetzlich abgelehnt (deny-by-default), damit ein vergessenes Secret nicht
   * versehentlich anonymen Schreibzugriff erlaubt.
   */
  private boolean isAuthorized(final String token) {
    if (token == null || incrementSecret.isBlank()) {
      return false;
    }
    return MessageDigest.isEqual(
        incrementSecret.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
  }
}
