enum PreferenceSection: CustomStringConvertible {
    
    case appearance
    case myShows
    case toWatch
    case specials
    
    var description: String {
        switch self {
        case .appearance: return string(R.str.prefs_appearance)
        case .myShows: return string(R.str.prefs_my_shows)
        case .toWatch: return string(R.str.prefs_to_watch)
        case .specials: return string(R.str.prefs_specials)
        }
    }
    
    var index: Int {
        PreferenceSection.allSections.firstIndex(of: self) ?? -1
    }
    
    static var allSections: [PreferenceSection] {
        if #available(iOS 13, *) {
            return [.appearance, .myShows, .toWatch, .specials]
        } else {
            return [.myShows, .toWatch, .specials]
        }
    }
    
    static func atIndex(_ index: Int) -> PreferenceSection {
        return allSections[index]
    }
}
