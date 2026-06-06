package org.mwolff.api.image.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

  private static final String SUB = "user-1";

  @Mock private ImageRepository repository;

  /** Echtes 1x1-PNG (per ImageIO erzeugt) — von Tika als image/png erkannt. */
  private static byte[] pngBytes() {
    try {
      final java.awt.image.BufferedImage img =
          new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      javax.imageio.ImageIO.write(img, "png", out);
      return out.toByteArray();
    } catch (final IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /** Minimales GIF — von Tika anhand der "GIF89a"-Signatur als image/gif erkannt. */
  private static byte[] gifBytes() {
    final byte[] header = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    return Arrays.copyOf(header, header.length + 8);
  }

  @Test
  void savesValidUploadWithDetectedTypeAndHash() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    final byte[] data = pngBytes();
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0, StoredImage.class));

    final StoredImage result = useCase.execute(SUB, data, "irrelevant.bin");

    final ArgumentCaptor<StoredImage> captor = ArgumentCaptor.forClass(StoredImage.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().userSub()).isEqualTo(SUB);
    assertThat(captor.getValue().contentType()).isEqualTo("image/png");
    assertThat(captor.getValue().hash()).isEqualTo(UploadImageUseCase.sha256Hex(data));
    assertThat(result.contentType()).isEqualTo("image/png");
    assertThat(result.hash()).isEqualTo(captor.getValue().hash());
  }

  @Test
  void detectsTypeFromBytesNotFromFilename() {
    // #231: PNG-Bytes, aber irreführender .jpg-Dateiname → trotzdem als image/png gespeichert.
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0, StoredImage.class));

    final StoredImage result = useCase.execute(SUB, pngBytes(), "lie.jpg");

    assertThat(result.contentType()).isEqualTo("image/png");
  }

  @Test
  void rejectsDisguisedNonImage() {
    // #231: echte PDF-Bytes mit .png-Dateiname → die Magic-Bytes (%PDF) schlagen den
    // Extension-Hint und verraten den Schwindel.
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    final byte[] pdf = "%PDF-1.4\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1);

    assertThatThrownBy(() -> useCase.execute(SUB, pdf, "evil.png"))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("UNSUPPORTED_TYPE");
    verify(repository, never()).save(any());
  }

  @Test
  void sha256HexComputesKnownDigest() {
    // SHA-256 von {0x61,0x62,0x63} = "abc".
    assertThat(UploadImageUseCase.sha256Hex(new byte[] {0x61, 0x62, 0x63}))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  void rejectsNullData() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    assertThatThrownBy(() -> useCase.execute(SUB, null, "x.png"))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("EMPTY_FILE");
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsEmptyData() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    assertThatThrownBy(() -> useCase.execute(SUB, new byte[0], "x.png"))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("EMPTY_FILE");
  }

  @Test
  void rejectsTooLargeData() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    final byte[] tooBig = new byte[UploadImageUseCase.MAX_SIZE_BYTES + 1];
    tooBig[0] = 1;
    assertThatThrownBy(() -> useCase.execute(SUB, tooBig, "x.png"))
        .isInstanceOf(InvalidImageUploadException.class)
        .extracting("code")
        .isEqualTo("TOO_LARGE");
    verify(repository, never()).save(any());
  }

  @Test
  void acceptsExactlyMaxSize() {
    // Grenzwert-Mutant (#203): genau MAX_SIZE_BYTES muss durchlaufen ( > MAX, nicht >= ).
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0, StoredImage.class));
    final byte[] png = pngBytes();
    final byte[] atLimit = Arrays.copyOf(png, UploadImageUseCase.MAX_SIZE_BYTES);

    assertThat(useCase.execute(SUB, atLimit, "x.png").sizeBytes())
        .isEqualTo(UploadImageUseCase.MAX_SIZE_BYTES);
  }

  @Test
  void acceptsWhitelistedTypesByMagicBytes() {
    final UploadImageUseCase useCase = new UploadImageUseCase(repository);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0, StoredImage.class));

    assertThat(useCase.execute(SUB, pngBytes(), "x.png").contentType()).isEqualTo("image/png");
    assertThat(useCase.execute(SUB, gifBytes(), "x.gif").contentType()).isEqualTo("image/gif");
  }
}
