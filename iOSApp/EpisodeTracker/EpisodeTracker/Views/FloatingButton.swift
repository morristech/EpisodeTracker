import UIKit
import MaterialComponents.MaterialButtons

class FloatingButton: MDCFloatingButton {
    
    let activityIndicator: UIActivityIndicatorView = {
        let indicator = UIActivityIndicatorView(style: UIActivityIndicatorView.Style.white)
        indicator.hidesWhenStopped = true
        indicator.color = .textColorPrimaryInverse
        return indicator
    }()
    
    @IBInspectable
    var image: UIImage? = nil
    
    var isActivityIndicatorHidden = true {
        didSet {
            if isActivityIndicatorHidden {
                hideActivityIndicator()
            } else {
                showActivityIndicator()
            }
        }
    }
    
    override init(frame: CGRect, shape: MDCFloatingButtonShape) {
        super.init(frame: frame, shape: shape)
        setup()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setup()
    }
    
    private func setup() {
        setTitleColor(.textColorPrimaryInverse, for: .normal)
        setTitleColor(.textColorPrimaryInverse, for: .disabled)
        disabledAlpha = 0.9
        setImage(image, for: .normal)
    }
    
    private func showActivityIndicator() {
        addSubview(activityIndicator)
        activityIndicator.center = imageView?.center ?? CGPoint(x: 0, y: 0)
        activityIndicator.startAnimating()
        setImage(nil, for: .normal)
    }
    
    private func hideActivityIndicator() {
        activityIndicator.stopAnimating()
        activityIndicator.removeFromSuperview()
        setImage(image, for: .normal)
    }
}
