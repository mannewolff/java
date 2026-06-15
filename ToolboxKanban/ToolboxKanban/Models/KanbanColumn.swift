//
//  KanbanColumn.swift
//  ToolboxKanban
//
//  Die vier Spalten eines Kanban-Boards, deckungsgleich mit dem Backend-Enum
//  (org.mwolff.api.kanban.domain.KanbanColumn).
//

import Foundation

enum KanbanColumn: String, Codable, CaseIterable, Identifiable {
  case backlog = "BACKLOG"
  case inProgress = "IN_PROGRESS"
  case inReview = "IN_REVIEW"
  case done = "DONE"

  var id: String { rawValue }

  /// Anzeigename für die UI.
  var displayName: String {
    switch self {
    case .backlog: return "Backlog"
    case .inProgress: return "In Arbeit"
    case .inReview: return "Review"
    case .done: return "Fertig"
    }
  }
}
