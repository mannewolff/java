package org.mwolff.api.timeseries.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

import org.mwolff.api.timeseries.domain.AggregateBucket;

/** Wire-Format eines Aggregations-Buckets. */
public record AggregateBucketResponse(
    Instant bucketStart,
    long count,
    BigDecimal min,
    BigDecimal max,
    BigDecimal avg,
    BigDecimal last) {

  public static AggregateBucketResponse from(AggregateBucket bucket) {
    return new AggregateBucketResponse(
        bucket.bucketStart(),
        bucket.count(),
        bucket.min(),
        bucket.max(),
        bucket.avg(),
        bucket.last());
  }
}
