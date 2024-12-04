import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    
    init() {
        Platform_iosKt.doInitKoinIos(networkStateProvider: IosNetworkHelper())
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
