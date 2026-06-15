//
//  TicketsViewModelTests.swift
//  ToolboxKanbanTests
//
//  Tests für #244: Ladezustände der Tickets-Liste.
//

import Testing

@testable import ToolboxKanban

@Suite(.serialized)
@MainActor
struct TicketsViewModelTests {

  @Test func startsIdle() {
    let vm = TicketsViewModel(
      service: MockKanbanService(result: .success([])), onUnauthorized: {})
    #expect(vm.state == .idle)
  }

  @Test func loadSuccessMovesToLoaded() async {
    let items = [KanbanItem.fixture(id: 1, title: "A"), .fixture(id: 2, title: "B")]
    let vm = TicketsViewModel(
      service: MockKanbanService(result: .success(items)), onUnauthorized: {})
    await vm.load()
    #expect(vm.state == .loaded(items))
  }

  @Test func loadFailureMovesToFailedWithMessage() async {
    let vm = TicketsViewModel(
      service: MockKanbanService(result: .failure(APIError.transport)), onUnauthorized: {})
    await vm.load()
    #expect(vm.state == .failed(APIError.transport.userMessage))
  }

  @Test func unauthorizedTriggersCallbackAndDoesNotSetLoaded() async {
    var calledBack = false
    let vm = TicketsViewModel(
      service: MockKanbanService(result: .failure(APIError.unauthorized)),
      onUnauthorized: { calledBack = true })
    await vm.load()
    #expect(calledBack == true)
    // State bleibt nicht auf .loaded — Re-Login erfolgt über den Callback.
    if case .loaded = vm.state { Issue.record("State darf bei 401 nicht .loaded sein") }
  }

  @Test func unknownErrorFallsBackToTransportMessage() async {
    struct Weird: Error {}
    let vm = TicketsViewModel(
      service: MockKanbanService(result: .failure(Weird())), onUnauthorized: {})
    await vm.load()
    #expect(vm.state == .failed(APIError.transport.userMessage))
  }

  // MARK: - Filter (#245)

  private func loadedViewModel() async -> TicketsViewModel {
    let items = [
      KanbanItem.fixture(id: 1, title: "B", column: .backlog),
      KanbanItem.fixture(id: 2, title: "P", column: .inProgress),
      KanbanItem.fixture(id: 3, title: "B2", column: .backlog),
      KanbanItem.fixture(id: 4, title: "D", column: .done),
    ]
    let vm = TicketsViewModel(
      service: MockKanbanService(result: .success(items)), onUnauthorized: {})
    await vm.load()
    return vm
  }

  @Test func filterNilShowsAllItems() async {
    let vm = await loadedViewModel()
    #expect(vm.selectedColumn == nil)
    #expect(vm.filteredItems.count == 4)
  }

  @Test func filterByColumnShowsOnlyMatchingItems() async {
    let vm = await loadedViewModel()
    vm.selectedColumn = .backlog
    #expect(vm.filteredItems.map(\.id) == [1, 3])
    vm.selectedColumn = .done
    #expect(vm.filteredItems.map(\.id) == [4])
  }

  @Test func filteredItemsEmptyWhenNotLoaded() {
    let vm = TicketsViewModel(
      service: MockKanbanService(result: .success([])), onUnauthorized: {})
    vm.selectedColumn = .backlog
    #expect(vm.filteredItems.isEmpty)
  }

  // MARK: - Erstellen (#246)

  @Test func createSucceedsAndReloads() async {
    let service = MockKanbanService(result: .success([.fixture(id: 9, title: "Neu")]))
    let vm = TicketsViewModel(service: service, onUnauthorized: {})
    let ok = await vm.create(title: "Neu", body: "Text", column: .inProgress)
    #expect(ok == true)
    #expect(service.createCallCount == 1)
    #expect(service.lastCreateArgs?.title == "Neu")
    #expect(service.lastCreateArgs?.column == .inProgress)
    // Liste wurde nach dem Anlegen neu geladen.
    #expect(service.fetchCallCount == 1)
    #expect(vm.state == .loaded([.fixture(id: 9, title: "Neu")]))
  }

  @Test func createUnauthorizedTriggersCallbackAndReturnsFalse() async {
    var calledBack = false
    let service = MockKanbanService(
      result: .success([]), createResult: .failure(APIError.unauthorized))
    let vm = TicketsViewModel(service: service, onUnauthorized: { calledBack = true })
    let ok = await vm.create(title: "X", body: "", column: .backlog)
    #expect(ok == false)
    #expect(calledBack == true)
    #expect(service.fetchCallCount == 0)
  }

  @Test func createFailureReturnsFalse() async {
    let service = MockKanbanService(
      result: .success([]), createResult: .failure(APIError.httpStatus(500)))
    let vm = TicketsViewModel(service: service, onUnauthorized: {})
    let ok = await vm.create(title: "X", body: "", column: .backlog)
    #expect(ok == false)
  }
}
