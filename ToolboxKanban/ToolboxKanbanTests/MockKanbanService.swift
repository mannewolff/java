//
//  MockKanbanService.swift
//  ToolboxKanbanTests
//
//  Stub von KanbanServiceProtocol für ViewModel-Tests.
//

import Foundation
@testable import ToolboxKanban

final class MockKanbanService: KanbanServiceProtocol {
  private let result: Result<[KanbanItem], Error>
  private(set) var fetchCallCount = 0

  init(result: Result<[KanbanItem], Error>) {
    self.result = result
  }

  func fetchItems() async throws -> [KanbanItem] {
    fetchCallCount += 1
    return try result.get()
  }
}

extension KanbanItem {
  /// Test-Fixture mit frei wählbaren Kernfeldern.
  static func fixture(
    id: Int = 1,
    title: String = "Test-Item",
    column: KanbanColumn = .backlog,
    createdAt: Date = Date(timeIntervalSinceReferenceDate: 0),
    number: Int = 1
  ) -> KanbanItem {
    KanbanItem(
      id: id, title: title, body: "", column: column, position: 0,
      createdAt: createdAt, updatedAt: createdAt, movedToDoneAt: nil,
      archived: false, number: number)
  }
}
