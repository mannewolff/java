package org.mwolff.api.image.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * {@link Pageable} mit beliebigem {@code offset} und {@code limit} (#198). Spring Datas {@code
 * PageRequest} kann nur seitenbasiert (offset = page * size) — diese Variante erlaubt einen frei
 * gewählten Offset, wie ihn die Galerie-Paginierung nutzt.
 */
final class OffsetLimitPageable implements Pageable {

  private final long offset;
  private final int limit;
  private final Sort sort;

  OffsetLimitPageable(final long offset, final int limit, final Sort sort) {
    if (offset < 0) {
      throw new IllegalArgumentException("offset must not be negative");
    }
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be >= 1");
    }
    this.offset = offset;
    this.limit = limit;
    this.sort = sort;
  }

  @Override
  public int getPageNumber() {
    return (int) (offset / limit);
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetLimitPageable(offset + limit, limit, sort);
  }

  @Override
  public Pageable previousOrFirst() {
    return hasPrevious()
        ? new OffsetLimitPageable(Math.max(0, offset - limit), limit, sort)
        : first();
  }

  @Override
  public Pageable first() {
    return new OffsetLimitPageable(0, limit, sort);
  }

  @Override
  public Pageable withPage(final int pageNumber) {
    return new OffsetLimitPageable((long) pageNumber * limit, limit, sort);
  }

  @Override
  public boolean hasPrevious() {
    return offset >= limit;
  }
}
