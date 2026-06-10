package org.mwolff.api.image.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class OffsetLimitPageableTest {

  @Test
  void exposesOffsetLimitAndDerivedPageNumber() {
    final OffsetLimitPageable p = new OffsetLimitPageable(20, 10, Sort.unsorted());
    assertThat(p.getOffset()).isEqualTo(20L);
    assertThat(p.getPageSize()).isEqualTo(10);
    assertThat(p.getPageNumber()).isEqualTo(2);
    assertThat(p.getSort()).isEqualTo(Sort.unsorted());
  }

  @Test
  void rejectsNegativeOffset() {
    assertThatThrownBy(() -> new OffsetLimitPageable(-1, 10, Sort.unsorted()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsLimitOfExactlyOne() {
    // Grenzwert: limit == 1 ist gueltig — killt ConditionalsBoundary auf limit < 1 (#207).
    final OffsetLimitPageable p = new OffsetLimitPageable(0, 1, Sort.unsorted());
    assertThat(p.getPageSize()).isEqualTo(1);
  }

  @Test
  void rejectsNonPositiveLimit() {
    assertThatThrownBy(() -> new OffsetLimitPageable(0, 0, Sort.unsorted()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nextAdvancesByLimit() {
    final OffsetLimitPageable next =
        (OffsetLimitPageable) new OffsetLimitPageable(0, 10, Sort.unsorted()).next();
    assertThat(next.getOffset()).isEqualTo(10L);
  }

  @Test
  void hasPreviousReflectsOffset() {
    assertThat(new OffsetLimitPageable(0, 10, Sort.unsorted()).hasPrevious()).isFalse();
    assertThat(new OffsetLimitPageable(10, 10, Sort.unsorted()).hasPrevious()).isTrue();
  }

  @Test
  void previousOrFirstStepsBackOrReturnsFirst() {
    assertThat(new OffsetLimitPageable(25, 10, Sort.unsorted()).previousOrFirst().getOffset())
        .isEqualTo(15L);
    assertThat(new OffsetLimitPageable(5, 10, Sort.unsorted()).previousOrFirst().getOffset())
        .isZero();
  }

  @Test
  void firstResetsOffsetToZero() {
    assertThat(new OffsetLimitPageable(40, 10, Sort.unsorted()).first().getOffset()).isZero();
  }

  @Test
  void withPageComputesOffsetFromPageNumber() {
    assertThat(new OffsetLimitPageable(0, 10, Sort.unsorted()).withPage(3).getOffset())
        .isEqualTo(30L);
  }
}
