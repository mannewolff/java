package org.mwolff.api.appversion.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class AppVersionRateLimiterTest {

  /** Manueller Clock — Tests können die Uhr weiterdrehen, ohne Real-Sleep. */
  private static final class MutableClock extends Clock {
    private final AtomicLong now;

    MutableClock(long startMillis) {
      this.now = new AtomicLong(startMillis);
    }

    void advance(long millis) {
      now.addAndGet(millis);
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public ZoneId getZone() {
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
    final AppVersionRateLimiter limiter = new AppVersionRateLimiter(3, 60_000L, clock);

    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isFalse();
  }

  @Test
  void resetsAfterWindow() {
    final MutableClock clock = new MutableClock(0);
    final AppVersionRateLimiter limiter = new AppVersionRateLimiter(2, 60_000L, clock);

    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isFalse();

    clock.advance(60_000L);

    assertThat(limiter.tryAcquire("k")).isTrue();
  }

  @Test
  void enforcesLimitWhenClockStartsAtNonZeroEpoch() {
    // Killt den Math-Mutanten now - startMillis -> now + startMillis (#207): mit Startzeit 0 ist
    // die Mutation nicht unterscheidbar; mit großer Epoch-Zeit würde der Mutant das Fenster bei
    // jedem Aufruf zurücksetzen und das Limit griffe nie.
    final MutableClock clock = new MutableClock(1_000_000L);
    final AppVersionRateLimiter limiter = new AppVersionRateLimiter(1, 60_000L, clock);

    assertThat(limiter.tryAcquire("k")).isTrue();
    assertThat(limiter.tryAcquire("k")).isFalse();
  }

  @Test
  void separateKeysHaveSeparateBuckets() {
    final MutableClock clock = new MutableClock(0);
    final AppVersionRateLimiter limiter = new AppVersionRateLimiter(1, 60_000L, clock);

    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isFalse();
    assertThat(limiter.tryAcquire("b")).isTrue();
  }
}
