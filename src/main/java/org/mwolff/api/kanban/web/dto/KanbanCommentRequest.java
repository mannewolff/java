package org.mwolff.api.kanban.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.mwolff.api.kanban.domain.KanbanComment;

/** Body für das Anlegen oder Bearbeiten eines Kommentars. */
public record KanbanCommentRequest(
    @NotBlank @Size(max = KanbanComment.MAX_BODY_LENGTH) String body) {}
