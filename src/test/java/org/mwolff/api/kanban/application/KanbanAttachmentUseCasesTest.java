package org.mwolff.api.kanban.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mwolff.api.kanban.domain.KanbanAttachment;
import org.mwolff.api.kanban.domain.KanbanAttachmentLimitExceededException;
import org.mwolff.api.kanban.domain.KanbanAttachmentMeta;
import org.mwolff.api.kanban.domain.KanbanAttachmentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanAttachmentPort;
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;

class KanbanAttachmentUseCasesTest {

  private static final String SUB = "user-1";
  private static final String OTHER = "user-2";
  private static final long ITEM_ID = 5L;
  private static final byte[] TEXT = "hello world".getBytes();
  // PDF-Magic-Bytes: Tika muss application/pdf erkennen, egal welchen Dateinamen der Client meldet.
  private static final byte[] PDF = "%PDF-1.5\n%âãÏÓ\n1 0 obj\n".getBytes();

  private final KanbanItemPort items = mock(KanbanItemPort.class);
  private final KanbanAttachmentPort attachments = mock(KanbanAttachmentPort.class);

  private static KanbanItem ownItem() {
    return KanbanItem.newInstance(SUB, "T", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH)
        .withNumber(1);
  }

  private static KanbanItem foreignItem() {
    return KanbanItem.newInstance(OTHER, "T", "", KanbanColumn.BACKLOG, 0, Instant.EPOCH)
        .withNumber(1);
  }

  // ----- upload -------------------------------------------------------------

  @Test
  void uploadRejectsUnknownItemAsNotFound() {
    given(items.findById(ITEM_ID)).willReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                new UploadAttachmentUseCase(items, attachments)
                    .execute(SUB, ITEM_ID, TEXT, "a.txt"))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(attachments, never()).save(any());
  }

  @Test
  void uploadRejectsForeignItemAsNotFound() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(foreignItem()));
    assertThatThrownBy(
            () ->
                new UploadAttachmentUseCase(items, attachments)
                    .execute(SUB, ITEM_ID, TEXT, "a.txt"))
        .isInstanceOf(KanbanItemNotFoundException.class);
  }

  @Test
  void uploadRejectsEmptyData() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    assertThatThrownBy(
            () ->
                new UploadAttachmentUseCase(items, attachments)
                    .execute(SUB, ITEM_ID, new byte[0], "a.txt"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void uploadRejectsNullData() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    assertThatThrownBy(
            () ->
                new UploadAttachmentUseCase(items, attachments)
                    .execute(SUB, ITEM_ID, null, "a.txt"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void uploadRejectsOversizeData() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    final byte[] big = new byte[UploadAttachmentUseCase.MAX_SIZE_BYTES + 1];
    big[0] = 1;
    assertThatThrownBy(
            () ->
                new UploadAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, big, "a.bin"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void uploadRejectsWhenLimitReached() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.countByItem(ITEM_ID))
        .willReturn((long) UploadAttachmentUseCase.MAX_ATTACHMENTS_PER_ITEM);
    assertThatThrownBy(
            () ->
                new UploadAttachmentUseCase(items, attachments)
                    .execute(SUB, ITEM_ID, TEXT, "a.txt"))
        .isInstanceOf(KanbanAttachmentLimitExceededException.class);
    verify(attachments, never()).save(any());
  }

  @Test
  void uploadAllowsExactlyAtLimitMinusOne() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.countByItem(ITEM_ID))
        .willReturn((long) (UploadAttachmentUseCase.MAX_ATTACHMENTS_PER_ITEM - 1));
    given(attachments.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanAttachment saved =
        new UploadAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, TEXT, "a.txt");

    assertThat(saved.uploadedBySub()).isEqualTo(SUB);
    verify(attachments).save(any());
  }

  @Test
  void uploadDetectsContentTypeFromBytesNotFilename() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.countByItem(ITEM_ID)).willReturn(0L);
    given(attachments.save(any())).willAnswer(inv -> inv.getArgument(0));

    // Client meldet ".txt", die Bytes sind aber ein PDF → Tika muss application/pdf speichern.
    final KanbanAttachment saved =
        new UploadAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, PDF, "harmless.txt");

    assertThat(saved.contentType()).isEqualTo("application/pdf");
    assertThat(saved.hash()).isNotBlank();
  }

  @Test
  void uploadSanitizesFilename() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.countByItem(ITEM_ID)).willReturn(0L);
    given(attachments.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanAttachment saved =
        new UploadAttachmentUseCase(items, attachments)
            .execute(SUB, ITEM_ID, TEXT, "a\r\nb\"c/..\\d.txt");

    assertThat(saved.filename()).isEqualTo("abc..d.txt");
  }

  @Test
  void sanitizeFilenameFallsBackToDownloadForNullOrBlank() {
    assertThat(UploadAttachmentUseCase.sanitizeFilename(null)).isEqualTo("download");
    assertThat(UploadAttachmentUseCase.sanitizeFilename("   ")).isEqualTo("download");
  }

  @Test
  void sanitizeFilenameTruncatesToMaxLength() {
    final String longName = "x".repeat(KanbanAttachment.MAX_FILENAME_LENGTH + 50);
    assertThat(UploadAttachmentUseCase.sanitizeFilename(longName))
        .hasSize(KanbanAttachment.MAX_FILENAME_LENGTH);
  }

  // ----- list ---------------------------------------------------------------

  @Test
  void listRejectsForeignItemAsNotFound() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(foreignItem()));
    assertThatThrownBy(() -> new ListAttachmentsUseCase(items, attachments).execute(SUB, ITEM_ID))
        .isInstanceOf(KanbanItemNotFoundException.class);
  }

  @Test
  void listReturnsMetaForOwnItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    final KanbanAttachmentMeta meta =
        new KanbanAttachmentMeta(1L, ITEM_ID, "a.txt", "text/plain", 3, SUB, Instant.EPOCH);
    given(attachments.findMetaByItem(ITEM_ID)).willReturn(List.of(meta));

    assertThat(new ListAttachmentsUseCase(items, attachments).execute(SUB, ITEM_ID))
        .containsExactly(meta);
  }

  // ----- get ----------------------------------------------------------------

  @Test
  void getRejectsForeignItemAsNotFound() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(foreignItem()));
    assertThatThrownBy(() -> new GetAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L))
        .isInstanceOf(KanbanItemNotFoundException.class);
  }

  @Test
  void getRejectsUnknownAttachment() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.findById(9L)).willReturn(Optional.empty());
    assertThatThrownBy(() -> new GetAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L))
        .isInstanceOf(KanbanAttachmentNotFoundException.class);
  }

  @Test
  void getRejectsAttachmentOfDifferentItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.findById(9L))
        .willReturn(
            Optional.of(KanbanAttachment.newInstance(999L, "a.txt", "text/plain", TEXT, "h", SUB)));
    assertThatThrownBy(() -> new GetAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L))
        .isInstanceOf(KanbanAttachmentNotFoundException.class);
  }

  @Test
  void getReturnsAttachmentForOwnItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    final KanbanAttachment a =
        KanbanAttachment.newInstance(ITEM_ID, "a.txt", "text/plain", TEXT, "h", SUB);
    given(attachments.findById(9L)).willReturn(Optional.of(a));

    assertThat(new GetAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L)).isEqualTo(a);
  }

  // ----- delete -------------------------------------------------------------

  @Test
  void deleteRejectsForeignItemAsNotFound() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(foreignItem()));
    assertThatThrownBy(
            () -> new DeleteAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(attachments, never()).deleteById(anyLong());
  }

  @Test
  void deleteRejectsUnknownAttachment() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.findById(9L)).willReturn(Optional.empty());
    assertThatThrownBy(
            () -> new DeleteAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L))
        .isInstanceOf(KanbanAttachmentNotFoundException.class);
    verify(attachments, never()).deleteById(anyLong());
  }

  @Test
  void deleteRejectsAttachmentOfDifferentItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.findById(9L))
        .willReturn(
            Optional.of(KanbanAttachment.newInstance(999L, "a", "text/plain", TEXT, "h", SUB)));
    assertThatThrownBy(
            () -> new DeleteAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L))
        .isInstanceOf(KanbanAttachmentNotFoundException.class);
    verify(attachments, never()).deleteById(anyLong());
  }

  @Test
  void deleteRemovesOwnAttachment() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownItem()));
    given(attachments.findById(9L))
        .willReturn(
            Optional.of(KanbanAttachment.newInstance(ITEM_ID, "a", "text/plain", TEXT, "h", SUB)));

    new DeleteAttachmentUseCase(items, attachments).execute(SUB, ITEM_ID, 9L);

    verify(attachments).deleteById(9L);
  }
}
