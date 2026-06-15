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
  @State private var showCreateSheet = false

  var body: some View {
    NavigationStack {
      Group {
        if let viewModel {
          @Bindable var vm = viewModel
          VStack(spacing: 0) {
            if case .loaded = vm.state {
              filterPicker(selection: $vm.selectedColumn)
            }
            content(vm)
          }
        } else {
          ProgressView("Tickets werden geladen…")
        }
      }
      .navigationTitle("Tickets")
      .toolbar {
        ToolbarItem(placement: .topBarLeading) {
          Button("Abmelden") { authState.logout() }
        }
        ToolbarItem(placement: .topBarTrailing) {
          Button {
            showCreateSheet = true
          } label: {
            Label("Neues Ticket", systemImage: "plus")
          }
          .disabled(viewModel == nil)
        }
      }
      .sheet(isPresented: $showCreateSheet) {
        CreateTicketView { title, body, column in
          await viewModel?.create(title: title, body: body, column: column) ?? false
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

  /// Segmentierter Filter über die vier Spalten plus „Alle" (#245).
  private func filterPicker(selection: Binding<KanbanColumn?>) -> some View {
    Picker("Status", selection: selection) {
      Text("Alle").tag(KanbanColumn?.none)
      ForEach(KanbanColumn.allCases) { column in
        Text(column.displayName).tag(KanbanColumn?.some(column))
      }
    }
    .pickerStyle(.segmented)
    .padding(.horizontal)
    .padding(.vertical, 8)
  }

  @ViewBuilder
  private func content(_ viewModel: TicketsViewModel) -> some View {
    switch viewModel.state {
    case .idle, .loading:
      ProgressView("Tickets werden geladen…")
    case .loaded:
      let items = viewModel.filteredItems
      if items.isEmpty {
        ContentUnavailableView(
          "Keine Tickets", systemImage: "tray",
          description: Text("Für diesen Filter sind keine Tickets vorhanden."))
      } else {
        List(items) { item in
          NavigationLink {
            TicketDetailView(item: item) { id in
              await viewModel.delete(id: id)
            }
          } label: {
            TicketRow(item: item)
          }
        }
        .listStyle(.plain)
      }
    case .failed(let message):
      ContentUnavailableView {
        Label("Fehler", systemImage: "exclamationmark.triangle")
      } description: {
        Text(message)
      } actions: {
        Button("Erneut versuchen") {
          Task { await viewModel.load() }
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
