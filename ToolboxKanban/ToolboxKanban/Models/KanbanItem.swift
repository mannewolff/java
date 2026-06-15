//
//  KanbanItem.swift
//  ToolboxKanban
//
//  Item-DTO, deckungsgleich mit dem Backend-Response (KanbanItemResponse).
//

import Foundation

struct KanbanItem: Identifiable, Equatable, Decodable {
  let id: Int
  let title: String
  let body: String
  let column: KanbanColumn
  let position: Int
  let createdAt: Date
  let updatedAt: Date
  /// Nur in der DONE-Spalte gesetzt.
  let movedToDoneAt: Date?
  let archived: Bool
  /// Fortlaufende, pro User eindeutige Anzeige-Nummer (#187/#188).
  let number: Int
}
