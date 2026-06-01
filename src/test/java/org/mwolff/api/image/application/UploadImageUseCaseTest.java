package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mwolff.api.image.domain.ImageRepository;
import org.mwolff.api.image.domain.InvalidImageUploadException;
import org.mwolff.api.image.domain.StoredImage;

@ExtendWith(MockitoExtension.class)
class UploadImageUseCaseTest {

  @Mock private ImageRepository repository;

  @Test
  void savesValidUpload() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    final byte[] data = {1, 2, 3};
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0, StoredImage.class));

    final StoredImage result = useCase.execute("image/png", data);

    final ArgumentCaptor<StoredImage> captor = ArgumentCaptor.forClass(StoredImage.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().contentType()).isEqualTo("image/png");
    assertThat(captor.getValue().data()).containsExactly(1, 2, 3);
    assertThat(result.sizeBytes()).isEqualTo(3);
  }

  @Test
  void rejectsNullData() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    assertThatThrownBy(() -> useCase.execute("image/png", null))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("EMPTY_FILE");
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsEmptyData() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    assertThatThrownBy(() -> useCase.execute("image/png", new byte[0]))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("EMPTY_FILE");
  }

  @Test
  void rejectsNullContentType() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    assertThatThrownBy(() -> useCase.execute(null, new byte[] {1}))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("UNSUPPORTED_TYPE");
  }

  @Test
  void rejectsUnsupportedContentType() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    assertThatThrownBy(() -> useCase.execute("application/pdf", new byte[] {1}))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("UNSUPPORTED_TYPE");
  }

  @Test
  void rejectsTooLargeData() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    final byte[] tooBig = new byte[UploadImageUseCase.MAX_SIZE_BYTES + 1];
    tooBig[0] = 1;
    assertThatThrownBy(() -> useCase.execute("image/jpeg", tooBig))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("TOO_LARGE");
    verify(repository, never()).save(any());
  }

  @Test
  void acceptsAllWhitelistedTypes() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0, StoredImage.class));
    for (final String type : new String[] {"image/jpeg", "image/png", "image/webp", "image/gif"}) {
      assertThat(useCase.execute(type, new byte[] {1}).contentType()).isEqualTo(type);
    }
  }
}
