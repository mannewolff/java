//
//  ToolboxKanbanApp.swift
//  ToolboxKanban
//

import SwiftUI

@main
struct ToolboxKanbanApp: App {
  @State private var authState = AuthState(authService: ASWebAuthSessionService())

  var body: some Scene {
    WindowGroup {
      ContentView()
        .environment(authState)
        .task { await authState.restore() }
    }
  }
}
