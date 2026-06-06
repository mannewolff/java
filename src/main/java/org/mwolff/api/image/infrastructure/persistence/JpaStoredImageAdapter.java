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
    entity.setUserSub(image.userSub());
    entity.setContentType(image.contentType());
    entity.setSizeBytes((int) image.sizeBytes());
    entity.setData(image.data());
    entity.setHash(image.hash());
    return toDomain(repository.save(entity));
  }

  @Override
  public Optional<StoredImage> findByIdAndUserSub(final long id, final String userSub) {
    return repository.findByIdAndUserSub(id, userSub).map(this::toDomain);
  }

  @Override
  public List<ImageMetadata> findMetadataByUserSub(
      final String userSub, final int limit, final int offset) {
    return repository
        .findByUserSubOrderByIdDesc(
            userSub, new OffsetLimitPageable(offset, limit, Sort.unsorted()))
        .stream()
        .map(JpaStoredImageAdapter::toMetadata)
        .toList();
  }

  @Override
  public long countByUserSub(final String userSub) {
    return repository.countByUserSub(userSub);
  }

  @Override
  public Optional<Long> findIdByHashAndUserSub(final String hash, final String userSub) {
    return repository.findIdsByHashAndUserSub(hash, userSub).stream().findFirst();
  }

  @Override
  public void delete(final long id) {
    repository.deleteById(id);
  }

  private static ImageMetadata toMetadata(final StoredImageMetadataView view) {
    return new ImageMetadata(
        view.getId(),
        view.getContentType(),
        view.getSizeBytes(),
        view.getCreatedAt(),
        view.getHash());
  }

  private StoredImage toDomain(final StoredImageEntity entity) {
    return new StoredImage(
        entity.getId(),
        entity.getUserSub(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getData(),
        entity.getCreatedAt(),
        entity.getHash());
  }
}
