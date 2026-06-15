//
//  TicketDetailView.swift
//  ToolboxKanban
//
//  Detailansicht eines Tickets mit Lösch-Funktion (#247). Löschen archiviert das
//  Item (Soft-Delete) nach Bestätigung; danach zurück zur Liste (die neu lädt).
//

import SwiftUI

struct TicketDetailView: View {
  let item: KanbanItem
  /// Archiviert das Item; liefert `true` bei Erfolg.
  let onDelete: (_ id: Int) async -> Bool

  @Environment(\.dismiss) private var dismiss
  @State private var showDeleteConfirm = false
  @State private var isDeleting = false
  @State private var showError = false

  var body: some View {
    List {
      Section {
        Text(item.title).font(.headline)
        if !item.body.isEmpty {
          Text(item.body)
        }
      }
      Section("Details") {
        LabeledContent("Nummer", value: "#\(item.number)")
        LabeledContent("Spalte", value: item.column.displayName)
        LabeledContent(
          "Erstellt",
          value: item.createdAt.formatted(date: .abbreviated, time: .shortened))
      }
      Section {
        Button(role: .destructive) {
          showDeleteConfirm = true
        } label: {
          if isDeleting {
            ProgressView()
          } else {
            Text("Ticket löschen")
          }
        }
        .disabled(isDeleting)
      }
    }
    .navigationTitle("#\(item.number)")
    .navigationBarTitleDisplayMode(.inline)
    .confirmationDialog(
      "Ticket löschen?", isPresented: $showDeleteConfirm, titleVisibility: .visible
    ) {
      Button("Löschen", role: .destructive) { Task { await delete() } }
      Button("Abbrechen", role: .cancel) {}
    } message: {
      Text("Das Ticket wird archiviert und aus der Liste entfernt.")
    }
    .alert("Löschen fehlgeschlagen", isPresented: $showError) {
      Button("OK", role: .cancel) {}
    } message: {
      Text("Das Ticket konnte nicht gelöscht werden. Bitte erneut versuchen.")
    }
  }

  private func delete() async {
    isDeleting = true
    let success = await onDelete(item.id)
    isDeleting = false
    if success {
      dismiss()
    } else {
      showError = true
    }
  }
}

#Preview {
  NavigationStack {
    TicketDetailView(
      item: .init(
        id: 1, title: "Login testen", body: "Gegen echten Keycloak prüfen.",
        column: .inProgress, position: 0, createdAt: .now, updatedAt: .now,
        movedToDoneAt: nil, archived: false, number: 7)
    ) { _ in true }
  }
}
