package org.mwolff.api.kanban.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.mwolff.api.kanban.domain.KanbanAccessToken;

/** Anlegen eines neuen Kanban-Access-Tokens. */
public record CreateKanbanTokenRequest(
    @NotBlank @Size(max = KanbanAccessToken.MAX_NAME_LENGTH) String name) {}
