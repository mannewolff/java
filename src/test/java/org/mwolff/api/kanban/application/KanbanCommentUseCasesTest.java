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
import org.mwolff.api.kanban.domain.KanbanColumn;
import org.mwolff.api.kanban.domain.KanbanComment;
import org.mwolff.api.kanban.domain.KanbanCommentForbiddenException;
import org.mwolff.api.kanban.domain.KanbanCommentNotFoundException;
import org.mwolff.api.kanban.domain.KanbanCommentPort;
import org.mwolff.api.kanban.domain.KanbanItem;
import org.mwolff.api.kanban.domain.KanbanItemNotFoundException;
import org.mwolff.api.kanban.domain.KanbanItemPort;

class KanbanCommentUseCasesTest {

  private static final String SUB_OWNER = "user-1";
  private static final String SUB_OTHER = "user-2";
  private static final String AUTHOR = "alice";
  private static final String OTHER_AUTHOR = "bob";
  private static final long ITEM_ID = 5L;
  private static final long COMMENT_ID = 9L;

  private final KanbanItemPort items = mock(KanbanItemPort.class);
  private final KanbanCommentPort comments = mock(KanbanCommentPort.class);

  private static KanbanItem ownedItem() {
    return new KanbanItem(
        ITEM_ID, SUB_OWNER, "T", "b", KanbanColumn.BACKLOG, 0, Instant.EPOCH, Instant.EPOCH, null);
  }

  private static KanbanComment comment(long itemId, String author) {
    return new KanbanComment(COMMENT_ID, itemId, author, "text", Instant.EPOCH, Instant.EPOCH);
  }

  // ----- add ----------------------------------------------------------------

  @Test
  void addPersistsCommentForOwnedItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanComment created =
        new AddCommentUseCase(items, comments).execute(SUB_OWNER, AUTHOR, ITEM_ID, "Hallo");

    assertThat(created.itemId()).isEqualTo(ITEM_ID);
    assertThat(created.author()).isEqualTo(AUTHOR);
    assertThat(created.body()).isEqualTo("Hallo");
  }

  @Test
  void addThrowsWhenItemMissing() {
    given(items.findById(ITEM_ID)).willReturn(Optional.empty());

    assertThatThrownBy(
            () -> new AddCommentUseCase(items, comments).execute(SUB_OWNER, AUTHOR, ITEM_ID, "x"))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(comments, never()).save(any());
  }

  @Test
  void addThrowsForForeignItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));

    assertThatThrownBy(
            () -> new AddCommentUseCase(items, comments).execute(SUB_OTHER, AUTHOR, ITEM_ID, "x"))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(comments, never()).save(any());
  }

  // ----- list ---------------------------------------------------------------

  @Test
  void listReturnsCommentsForOwnedItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findByItemNewestFirst(ITEM_ID)).willReturn(List.of(comment(ITEM_ID, AUTHOR)));

    final List<KanbanComment> result =
        new ListCommentsUseCase(items, comments).execute(SUB_OWNER, ITEM_ID);

    assertThat(result).hasSize(1);
  }

  @Test
  void listThrowsForForeignItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));

    assertThatThrownBy(() -> new ListCommentsUseCase(items, comments).execute(SUB_OTHER, ITEM_ID))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(comments, never()).findByItemNewestFirst(anyLong());
  }

  // ----- update -------------------------------------------------------------

  @Test
  void updatePersistsNewBodyForOwnComment() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.of(comment(ITEM_ID, AUTHOR)));
    given(comments.save(any())).willAnswer(inv -> inv.getArgument(0));

    final KanbanComment updated =
        new UpdateCommentUseCase(items, comments)
            .execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID, "neu");

    assertThat(updated.body()).isEqualTo("neu");
  }

  @Test
  void updateThrowsForForeignItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));

    assertThatThrownBy(
            () ->
                new UpdateCommentUseCase(items, comments)
                    .execute(SUB_OTHER, AUTHOR, ITEM_ID, COMMENT_ID, "x"))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(comments, never()).save(any());
  }

  @Test
  void updateThrowsWhenCommentMissing() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new UpdateCommentUseCase(items, comments)
                    .execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID, "x"))
        .isInstanceOf(KanbanCommentNotFoundException.class);
    verify(comments, never()).save(any());
  }

  @Test
  void updateThrowsWhenCommentBelongsToDifferentItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.of(comment(ITEM_ID + 1, AUTHOR)));

    assertThatThrownBy(
            () ->
                new UpdateCommentUseCase(items, comments)
                    .execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID, "x"))
        .isInstanceOf(KanbanCommentNotFoundException.class);
    verify(comments, never()).save(any());
  }

  @Test
  void updateThrowsForForeignAuthor() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.of(comment(ITEM_ID, OTHER_AUTHOR)));

    assertThatThrownBy(
            () ->
                new UpdateCommentUseCase(items, comments)
                    .execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID, "x"))
        .isInstanceOf(KanbanCommentForbiddenException.class);
    verify(comments, never()).save(any());
  }

  // ----- delete -------------------------------------------------------------

  @Test
  void deleteRemovesOwnComment() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.of(comment(ITEM_ID, AUTHOR)));

    new DeleteCommentUseCase(items, comments).execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID);

    verify(comments).deleteById(COMMENT_ID);
  }

  @Test
  void deleteThrowsForForeignItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));

    assertThatThrownBy(
            () ->
                new DeleteCommentUseCase(items, comments)
                    .execute(SUB_OTHER, AUTHOR, ITEM_ID, COMMENT_ID))
        .isInstanceOf(KanbanItemNotFoundException.class);
    verify(comments, never()).deleteById(anyLong());
  }

  @Test
  void deleteThrowsWhenCommentMissing() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new DeleteCommentUseCase(items, comments)
                    .execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID))
        .isInstanceOf(KanbanCommentNotFoundException.class);
    verify(comments, never()).deleteById(anyLong());
  }

  @Test
  void deleteThrowsWhenCommentBelongsToDifferentItem() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.of(comment(ITEM_ID + 1, AUTHOR)));

    assertThatThrownBy(
            () ->
                new DeleteCommentUseCase(items, comments)
                    .execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID))
        .isInstanceOf(KanbanCommentNotFoundException.class);
    verify(comments, never()).deleteById(anyLong());
  }

  @Test
  void deleteThrowsForForeignAuthor() {
    given(items.findById(ITEM_ID)).willReturn(Optional.of(ownedItem()));
    given(comments.findById(COMMENT_ID)).willReturn(Optional.of(comment(ITEM_ID, OTHER_AUTHOR)));

    assertThatThrownBy(
            () ->
                new DeleteCommentUseCase(items, comments)
                    .execute(SUB_OWNER, AUTHOR, ITEM_ID, COMMENT_ID))
        .isInstanceOf(KanbanCommentForbiddenException.class);
    verify(comments, never()).deleteById(anyLong());
  }
}
