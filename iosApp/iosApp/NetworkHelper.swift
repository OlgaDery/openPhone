
import Foundation
import Network
import ComposeApp

class IosNetworkHelper : INetworkStateProvider {
    
    private var networkStateProvider: NetworkStateCallbackProvider? = nil
    
    private let monitor: NWPathMonitor = NWPathMonitor()
    
    func setCallbackProviderForPlatform(provider: any NetworkStateCallbackProvider) {
        networkStateProvider = provider
    }
    
    func subscribe(completionHandler: @escaping ((any Error)?) -> Void) {
        monitor.pathUpdateHandler = { path in
            if path.status == .satisfied {
                self.networkStateProvider?.onResultReceived(state: NetworkStates.connected)
            } else {
                self.networkStateProvider?.onResultReceived(state: NetworkStates.lost)
            }
        }
        monitor.start(queue: DispatchQueue.global(qos: .background))
    }
    
    func unsubscribe() {
        monitor.cancel()
    }
}
