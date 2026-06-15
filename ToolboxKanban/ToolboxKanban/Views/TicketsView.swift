//
//  TicketsView.swift
//  ToolboxKanban
//
//  Tickets-Liste (#244): lädt die Kanban-Items des angemeldeten Users und zeigt
//  sie mit Titel, Spalte/Status und Erstelldatum. Loading- und Fehlerzustand inklusive.
//

import SwiftUI

struct TicketsView: View {
  @Environment(AuthState.self) private var authState
  @State private var viewModel: TicketsViewModel?

  var body: some View {
    NavigationStack {
      content
        .navigationTitle("Tickets")
        .toolbar {
          ToolbarItem(placement: .topBarTrailing) {
            Button("Abmelden") { authState.logout() }
          }
        }
    }
    .task {
      if viewModel == nil {
        viewModel = makeViewModel()
      }
      await viewModel?.load()
    }
  }

  @ViewBuilder
  private var content: some View {
    switch viewModel?.state ?? .idle {
    case .idle, .loading:
      ProgressView("Tickets werden geladen…")
    case .loaded(let items):
      if items.isEmpty {
        ContentUnavailableView(
          "Keine Tickets", systemImage: "tray",
          description: Text("Es sind noch keine Tickets vorhanden."))
      } else {
        List(items) { TicketRow(item: $0) }
          .listStyle(.plain)
      }
    case .failed(let message):
      ContentUnavailableView {
        Label("Fehler", systemImage: "exclamationmark.triangle")
      } description: {
        Text(message)
      } actions: {
        Button("Erneut versuchen") {
          Task { await viewModel?.load() }
        }
      }
    }
  }

  /// Baut das ViewModel mit dem echten Service; Token kommt aus AuthState,
  /// 401 löst Logout (Re-Login) aus.
  private func makeViewModel() -> TicketsViewModel {
    let service = KanbanService(tokenProvider: { await authState.validAccessToken() })
    return TicketsViewModel(service: service, onUnauthorized: { authState.logout() })
  }
}

/// Eine Zeile der Tickets-Liste: Nummer, Titel, Spalte und Datum.
struct TicketRow: View {
  let item: KanbanItem

  var body: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(item.title)
        .font(.headline)
        .lineLimit(2)
      HStack(spacing: 8) {
        Text("#\(item.number)")
          .foregroundStyle(.secondary)
        Text(item.column.displayName)
          .font(.caption.bold())
          .padding(.horizontal, 8)
          .padding(.vertical, 2)
          .background(.tint.opacity(0.15), in: Capsule())
        Spacer()
        Text(item.createdAt.formatted(date: .abbreviated, time: .omitted))
          .foregroundStyle(.secondary)
      }
      .font(.caption)
    }
    .padding(.vertical, 4)
  }
}

#Preview {
  let items = [
    KanbanItem(
      id: 1, title: "Login-Flow testen", body: "", column: .inProgress, position: 0,
      createdAt: .now, updatedAt: .now, movedToDoneAt: nil, archived: false, number: 7),
    KanbanItem(
      id: 2, title: "Keychain anbinden", body: "", column: .done, position: 0,
      createdAt: .now, updatedAt: .now, movedToDoneAt: .now, archived: false, number: 6),
  ]
  return List(items) { TicketRow(item: $0) }
}
