import SwiftUI
import UIKit
import Shared

/// Hosts the shared Compose UI. The OS draws the status and home-indicator bars;
/// Compose pads content with WindowInsets.safeDrawing.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
