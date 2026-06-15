//
//  CreateTicketView.swift
//  ToolboxKanban
//
//  Formular zum Anlegen eines neuen Kanban-Items (#246). Validiert, dass der Titel
//  nicht leer ist; bei Erfolg wird das Sheet geschlossen (die Liste lädt neu).
//

import SwiftUI

struct CreateTicketView: View {
  /// Übernimmt das Anlegen; liefert `true` bei Erfolg.
  let onCreate: (_ title: String, _ body: String, _ column: KanbanColumn) async -> Bool

  @Environment(\.dismiss) private var dismiss
  @State private var title = ""
  @State private var descriptionText = ""
  @State private var column: KanbanColumn = .backlog
  @State private var isSaving = false
  @State private var showError = false

  private var trimmedTitle: String {
    title.trimmingCharacters(in: .whitespacesAndNewlines)
  }
  private var canSave: Bool { !trimmedTitle.isEmpty && !isSaving }

  var body: some View {
    NavigationStack {
      Form {
        Section("Titel") {
          TextField("Titel", text: $title)
        }
        Section("Beschreibung") {
          TextEditor(text: $descriptionText)
            .frame(minHeight: 120)
        }
        Section("Spalte") {
          Picker("Spalte", selection: $column) {
            ForEach(KanbanColumn.allCases) { Text($0.displayName).tag($0) }
          }
        }
      }
      .navigationTitle("Neues Ticket")
      .navigationBarTitleDisplayMode(.inline)
      .toolbar {
        ToolbarItem(placement: .cancellationAction) {
          Button("Abbrechen") { dismiss() }
        }
        ToolbarItem(placement: .confirmationAction) {
          if isSaving {
            ProgressView()
          } else {
            Button("Anlegen") { Task { await save() } }
              .disabled(!canSave)
          }
        }
      }
      .alert("Anlegen fehlgeschlagen", isPresented: $showError) {
        Button("OK", role: .cancel) {}
      } message: {
        Text("Das Ticket konnte nicht angelegt werden. Bitte erneut versuchen.")
      }
    }
  }

  private func save() async {
    isSaving = true
    let success = await onCreate(trimmedTitle, descriptionText, column)
    isSaving = false
    if success {
      dismiss()
    } else {
      showError = true
    }
  }
}

#Preview {
  CreateTicketView { _, _, _ in true }
}
