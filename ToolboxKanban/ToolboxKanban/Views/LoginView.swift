//
//  LoginView.swift
//  ToolboxKanban
//
//  Einstiegsscreen für nicht-eingeloggte User. Startet den Keycloak-OIDC-Flow.
//

import SwiftUI

struct LoginView: View {
  @Environment(AuthState.self) private var authState

  var body: some View {
    VStack(spacing: 32) {
      Spacer()

      Image(systemName: "checklist")
        .font(.system(size: 72))
        .foregroundStyle(.tint)

      VStack(spacing: 8) {
        Text("Toolbox Kanban")
          .font(.largeTitle.bold())
        Text("Bitte melde dich an, um fortzufahren.")
          .foregroundStyle(.secondary)
      }

      Spacer()

      Button {
        Task { await authState.login() }
      } label: {
        Group {
          if authState.isLoading {
            ProgressView()
              .tint(.white)
          } else {
            Text("Mit Keycloak anmelden")
              .bold()
          }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
      }
      .buttonStyle(.borderedProminent)
      .disabled(authState.isLoading)
      .padding(.horizontal)

      if let error = authState.loginError {
        Text(error.localizedDescription)
          .font(.footnote)
          .foregroundStyle(.red)
          .multilineTextAlignment(.center)
          .padding(.horizontal)
      }

      Spacer()
    }
    .padding()
  }
}

#Preview {
  LoginView()
    .environment(AuthState(authService: PreviewAuthService()))
}

/// Fake-Service für SwiftUI-Previews — simuliert erfolgreichen Login nach 1 Sekunde.
private final class PreviewAuthService: AuthServiceProtocol {
  func authenticate() async throws -> TokenResponse {
    try await Task.sleep(nanoseconds: 1_000_000_000)
    return TokenResponse(
      accessToken: "preview-token", refreshToken: nil, idToken: nil,
      expiresIn: 300, tokenType: "Bearer", scope: "openid")
  }
}
