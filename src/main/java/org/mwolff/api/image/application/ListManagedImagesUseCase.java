package org.mwolff.api.image.application;

import java.util.List;
import java.util.Map;

import org.mwolff.api.image.domain.ImageMetadata;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.ImageUsagePort;
import org.mwolff.api.image.domain.ManagedImage;
import org.mwolff.api.image.domain.ManagedImagePage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listet Bilder paginiert inklusive Verwendungszähler für den Image-Manager (#202). Die
 * Verwendungszahlen stammen aus der dokumentierten Kante {@code image → dashboard}.
 */
@Component
public class ListManagedImagesUseCase {

  private final ImageRepository repository;
  private final ImageUsagePort usagePort;

  public ListManagedImagesUseCase(
      final ImageRepository repository, final ImageUsagePort usagePort) {
    this.repository = repository;
    this.usagePort = usagePort;
  }

  @Transactional(readOnly = true)
  public ManagedImagePage execute(final Integer limit, final Integer offset) {
    final int effectiveLimit =
        limit == null
            ? ListImagesUseCase.DEFAULT_LIMIT
            : Math.min(Math.max(1, limit), ListImagesUseCase.MAX_LIMIT);
    final int effectiveOffset = offset == null ? 0 : Math.max(0, offset);

    final List<ImageMetadata> metadata = repository.findMetadata(effectiveLimit, effectiveOffset);
    final Map<Long, Long> usage = usagePort.usageCounts();
    final List<ManagedImage> managed =
        metadata.stream().map(m -> new ManagedImage(m, usage.getOrDefault(m.id(), 0L))).toList();
    return new ManagedImagePage(managed, repository.count());
  }
}
