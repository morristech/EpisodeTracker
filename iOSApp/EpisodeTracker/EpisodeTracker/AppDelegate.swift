import UIKit
import Kingfisher
import SharedCode

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    static func instance() -> AppDelegate {
        return UIApplication.shared.delegate as! AppDelegate
    }

    var window: UIWindow?
    
    let database: Database = DatabaseKt.database
    let analytics: Analytics = AnalyticsImpl()
    let tmdbService: TmdbService = TmdbService()
    let omdbService: OmdbService = OmdbService()
    let connectivity: Connectivity = ConnectivityImpl()
    lazy var preferences: Preferences = Preferences(settings: SettingsKt.settings, analytics: analytics)
    lazy var showRepository: ShowRepository = ShowRepository(tmdbService: tmdbService, omdbService: omdbService, db: database, preferences: preferences)
    lazy var addToMyShowsQueue: AddToMyShowsQueue = AddToMyShowsQueue(db: database, tmdbService: tmdbService, connectivity: connectivity, showRepository: showRepository)
    lazy var myShowsRepository: MyShowsRepository = MyShowsRepository(db: database, addToMyShowsQueue: addToMyShowsQueue)
    lazy var discoverRepository: DiscoverRepository = DiscoverRepository(tmdbService: tmdbService)
    lazy var toWatchRepository: ToWatchRepository = ToWatchRepository(db: database, preferences: preferences)
    lazy var episodesRepository: EpisodesRepository = EpisodesRepository(db: database)
    
    lazy var presenter = AppPresenter(preferences: preferences, myShowsRepository: myShowsRepository, showRepository: showRepository, analytics: analytics)

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        setupImageCache()
        
        presenter.attachView(view: self)
        
        return true
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        presenter.onViewAppeared()
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        presenter.onViewDisappeared()
    }
    
    @available(iOS 13.0, *)
    func applyAppearance(_ appearance: Appearance) {
        var style: UIUserInterfaceStyle
        switch appearance {
        case .automatic:
            style = .unspecified
        case .light:
            style = .light
        case .dark:
            style = .dark
        default:
            fatalError("Unknown appearance: \(appearance)")
        }
        window?.overrideUserInterfaceStyle = style
    }
    
    private func setupImageCache() {
        let cache = ImageCache.default
        
        cache.memoryStorage.config.totalCostLimit = 100 * 1024 * 1024
        cache.memoryStorage.config.expiration = .never
        
        cache.diskStorage.config.sizeLimit = 200 * 1024 * 1024
        cache.diskStorage.config.expiration = .days(30)
        
        ImageCache.default.calculateDiskStorageSize { result in
            switch result {
            case .success(let size):
                log(self) { "Disk image cache size: \(size / 1024 / 1024) MB" }
            case .failure(let error):
                log(self) { "\(error)" }
            }
        }
    }
}

// MARK: - AppView implementation
extension AppDelegate: AppView {
    
    func setAppearance(appearance: Appearance) {
        if #available(iOS 13.0, *) {
            applyAppearance(appearance)
        }
    }
}
