package org.mwolff.api.ingest.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class IngestRateLimiterTest {

  /** Manueller Clock — Tests koennen die Uhr weiterdrehen, ohne Real-Sleep. */
  private static final class MutableClock extends Clock {
    private final AtomicLong now;

    MutableClock(long startMillis) {
      this.now = new AtomicLong(startMillis);
    }

    void advance(long millis) {
      now.addAndGet(millis);
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(now.get());
    }
  }

  @Test
  void allowsRequestsUpToCapacity() {
    final MutableClock clock = new MutableClock(0);
    final IngestRateLimiter limiter = new IngestRateLimiter(3, 60_000L, clock);

    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isFalse();
  }

  @Test
  void resetsAfterWindow() {
    final MutableClock clock = new MutableClock(0);
    final IngestRateLimiter limiter = new IngestRateLimiter(2, 60_000L, clock);

    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isFalse();

    clock.advance(60_000L);

    assertThat(limiter.tryAcquire("k")).isTrue();
  }

  @Test
  void enforcesLimitWhenClockStartsAtNonZeroEpoch() {
    // Killt den Math-Mutanten now - startMillis -> now + startMillis (#207): mit Startzeit 0
    // ist die Mutation nicht unterscheidbar; mit grosser Epoch-Zeit wuerde der Mutant das
    // Fenster bei jedem Aufruf zuruecksetzen und das Limit griffe nie.
    final MutableClock clock = new MutableClock(1_000_000L);
    final IngestRateLimiter limiter = new IngestRateLimiter(1, 60_000L, clock);

    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isFalse();
  }

  @Test
  void separateKeysHaveSeparateBuckets() {
    final MutableClock clock = new MutableClock(0);
    final IngestRateLimiter limiter = new IngestRateLimiter(1, 60_000L, clock);

    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isFalse();
    assertThat(limiter.tryAcquire("b")).isTrue();
  }

  @Test
  void resetForTestClearsBuckets() {
    final MutableClock clock = new MutableClock(0);
    final IngestRateLimiter limiter = new IngestRateLimiter(1, 60_000L, clock);

    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isFalse();

    limiter.resetForTest();

    assertThat(limiter.tryAcquire("k")).isTrue();
  }
}
