package org.mwolff.api.ingest.infrastructure.ratelimit;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-Memory-Rate-Limiter fuer die Public-Ingest-Schnittstelle. Pro Schluessel (Token-Hash oder IP)
 * ein Fixed-Window von {@code window} ms mit {@code capacity} Requests.
 *
 * <p>Single-Process-Limit — bei Mehr-Instanz-Deployment muesste eine zentrale Variante (Redis,
 * Spring-Bucket4j-DistributedCache, …) her. Fuer den Solo-Server der Toolbox reicht es.
 */
@Component
public class IngestRateLimiter {

  private final int capacity;
  private final long windowMillis;
  private final Clock clock;
  private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

  public IngestRateLimiter(
      @Value("${toolbox.ingest.rate-limit.capacity:60}") int capacity,
      @Value("${toolbox.ingest.rate-limit.window-millis:60000}") long windowMillis,
      Clock clock) {
    this.capacity = capacity;
    this.windowMillis = windowMillis;
    this.clock = clock;
  }

  /**
   * Versucht, einen Request fuer den Schluessel zu akzeptieren. Liefert {@code true} bei Erfolg
   * (Request darf durch), {@code false} bei Limit-Ueberschreitung.
   */
  public boolean tryAcquire(String key) {
    final Window window = windows.computeIfAbsent(key, k -> new Window());
    synchronized (window) {
      final long now = clock.millis();
      if (now - window.startMillis >= windowMillis) {
        window.startMillis = now;
        window.count = 0;
      }
      // Saturierend bei capacity+1 inkrementieren (kein Overflow bei Dauerfeuer) und das
      // Ergebnis EINMAL berechnet zurueckgeben — konstante true/false-Returns waeren
      // identische, durch keinen Test killbare PIT-Mutanten (#207).
      window.count = Math.min(window.count + 1, capacity + 1);
      return window.count <= capacity;
    }
  }

  /** Nur fuer Tests, raeumt alle Buckets. */
  void resetForTest() {
    windows.clear();
  }

  private static final class Window {
    long startMillis;
    int count;
  }
}
