import UIKit

extension URL {
    
    func open() {
        UIApplication.shared.open(self, options: [:], completionHandler: nil)
    }
    
    func canBeOpen() -> Bool {
        return UIApplication.shared.canOpenURL(self)
    }
}

extension String {
    func toUrl() -> URL? {
        return URL(string: self)
    }
}
