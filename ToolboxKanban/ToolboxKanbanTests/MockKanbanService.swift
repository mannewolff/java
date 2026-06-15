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
  private let createResult: Result<KanbanItem, Error>
  private let deleteResult: Result<Void, Error>
  private(set) var fetchCallCount = 0
  private(set) var createCallCount = 0
  private(set) var lastCreateArgs: (title: String, body: String, column: KanbanColumn)?
  private(set) var deleteCallCount = 0
  private(set) var lastDeletedId: Int?

  init(
    result: Result<[KanbanItem], Error>,
    createResult: Result<KanbanItem, Error> = .success(.fixture()),
    deleteResult: Result<Void, Error> = .success(())
  ) {
    self.result = result
    self.createResult = createResult
    self.deleteResult = deleteResult
  }

  func fetchItems() async throws -> [KanbanItem] {
    fetchCallCount += 1
    return try result.get()
  }

  func createItem(title: String, body: String, column: KanbanColumn) async throws -> KanbanItem {
    createCallCount += 1
    lastCreateArgs = (title, body, column)
    return try createResult.get()
  }

  func deleteItem(id: Int) async throws {
    deleteCallCount += 1
    lastDeletedId = id
    try deleteResult.get()
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
