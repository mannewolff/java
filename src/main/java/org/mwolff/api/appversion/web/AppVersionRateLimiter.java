package org.mwolff.api.appversion.web;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fixed-Window-Rate-Limiter für die mutierenden Version-Endpunkte (#311). Die Increment-POSTs sind
 * auf Security-Ebene {@code permitAll} und nur per Shared-Secret-Header geschützt — ohne Drosselung
 * wäre das Secret internetweit brute-forcebar. Ein globales Fenster (ein Schlüssel) drosselt alle
 * Versuche gemeinsam; legitime Deploy-Aufrufe sind selten und bleiben unter dem Limit.
 *
 * <p>Single-Process-Limit — für den Solo-Server der Toolbox ausreichend (analog {@code
 * IngestRateLimiter}).
 */
@Component
public class AppVersionRateLimiter {

  private final int capacity;
  private final long windowMillis;
  private final Clock clock;
  private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

  public AppVersionRateLimiter(
      @Value("${app.version.rate-limit.capacity:20}") int capacity,
      @Value("${app.version.rate-limit.window-millis:60000}") long windowMillis,
      Clock clock) {
    this.capacity = capacity;
    this.windowMillis = windowMillis;
    this.clock = clock;
  }

  /**
   * Versucht, einen Request für den Schlüssel zu akzeptieren. Liefert {@code true} bei Erfolg,
   * {@code false} bei Überschreitung des Fensters.
   */
  public boolean tryAcquire(String key) {
    final Window window = windows.computeIfAbsent(key, k -> new Window());
    synchronized (window) {
      final long now = clock.millis();
      if (now - window.startMillis >= windowMillis) {
        window.startMillis = now;
        window.count = 0;
      }
      // Saturierend bei capacity+1 inkrementieren (kein Overflow bei Dauerfeuer) und das Ergebnis
      // einmal berechnet zurückgeben — konstante true/false-Returns wären identische, durch keinen
      // Test killbare PIT-Mutanten (analog IngestRateLimiter, #207).
      window.count = Math.min(window.count + 1, capacity + 1);
      return window.count <= capacity;
    }
  }

  private static final class Window {
    long startMillis;
    int count;
  }
}
