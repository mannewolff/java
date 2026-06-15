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
}
