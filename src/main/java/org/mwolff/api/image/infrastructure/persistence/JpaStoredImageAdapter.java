package org.mwolff.api.image.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.mwolff.api.image.domain.ImageMetadata;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.StoredImage;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Persistenz-Adapter: bildet {@link StoredImage} auf {@link StoredImageEntity} ab (#181). */
@Component
class JpaStoredImageAdapter implements ImageRepository {

  private final StoredImageJpaRepository repository;

  JpaStoredImageAdapter(final StoredImageJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public StoredImage save(final StoredImage image) {
    final StoredImageEntity entity = new StoredImageEntity();
    entity.setContentType(image.contentType());
    entity.setSizeBytes((int) image.sizeBytes());
    entity.setData(image.data());
    return toDomain(repository.save(entity));
  }

  @Override
  public Optional<StoredImage> findById(final long id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public List<ImageMetadata> findMetadata(final int limit, final int offset) {
    return repository
        .findAllByOrderByIdDesc(new OffsetLimitPageable(offset, limit, Sort.unsorted()))
        .stream()
        .map(JpaStoredImageAdapter::toMetadata)
        .toList();
  }

  @Override
  public long count() {
    return repository.count();
  }

  // hash bleibt bis zur Duplikat-Erkennung (#199) null.
  private static ImageMetadata toMetadata(final StoredImageMetadataView view) {
    return new ImageMetadata(
        view.getId(), view.getContentType(), view.getSizeBytes(), view.getCreatedAt(), null);
  }

  private StoredImage toDomain(final StoredImageEntity entity) {
    return new StoredImage(
        entity.getId(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getData(),
        entity.getCreatedAt());
  }
}
