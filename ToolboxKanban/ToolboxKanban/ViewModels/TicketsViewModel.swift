//
//  TicketsViewModel.swift
//  ToolboxKanban
//
//  Hält den Ladezustand der Tickets-Liste (#244). Bei 401 wird onUnauthorized
//  ausgelöst (führt zum Re-Login), sonstige Fehler landen als Meldung im State.
//

import Foundation
import Observation

@MainActor
@Observable
final class TicketsViewModel {
  enum State: Equatable {
    case idle
    case loading
    case loaded([KanbanItem])
    case failed(String)
  }

  private(set) var state: State = .idle

  /// Aktiver Spalten-Filter; `nil` = alle Spalten anzeigen (#245).
  var selectedColumn: KanbanColumn?

  /// Die nach `selectedColumn` gefilterten Items des `.loaded`-Zustands.
  /// Die Sortierung (neueste zuerst) liefert bereits der Service.
  var filteredItems: [KanbanItem] {
    guard case .loaded(let items) = state else { return [] }
    guard let column = selectedColumn else { return items }
    return items.filter { $0.column == column }
  }

  private let service: any KanbanServiceProtocol
  /// Wird bei 401/abgelaufener Sitzung aufgerufen (i. d. R. AuthState.logout).
  private let onUnauthorized: () -> Void

  init(service: any KanbanServiceProtocol, onUnauthorized: @escaping () -> Void) {
    self.service = service
    self.onUnauthorized = onUnauthorized
  }

  /// Lädt die Items. Setzt `state` auf `.loading` und anschließend auf das Ergebnis.
  func load() async {
    state = .loading
    do {
      let items = try await service.fetchItems()
      state = .loaded(items)
    } catch APIError.unauthorized {
      onUnauthorized()
    } catch let error as APIError {
      state = .failed(error.userMessage)
    } catch {
      state = .failed(APIError.transport.userMessage)
    }
  }
}
