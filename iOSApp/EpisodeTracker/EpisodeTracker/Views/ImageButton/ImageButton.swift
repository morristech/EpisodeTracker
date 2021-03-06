import UIKit
import MaterialComponents.MaterialRipple

@IBDesignable
class ImageButton: UIView {
    
    @IBOutlet var contentView: UIView!
    @IBOutlet weak var imageView: UIImageView!
    @IBOutlet weak var activityIndicator: UIActivityIndicatorView!
    @IBOutlet weak var leftConstraint: NSLayoutConstraint!
    @IBOutlet weak var rightConstraint: NSLayoutConstraint!
    @IBOutlet weak var topConstraint: NSLayoutConstraint!
    @IBOutlet weak var bottomConstraint: NSLayoutConstraint!
    
    @IBInspectable
    var image: UIImage? {
        didSet {
            updateImage()
        }
    }
    
    @IBInspectable
    var isCircular: Bool = false {
        didSet {
            updateCornerRadius()
        }
    }
    
    var isActivityIndicatorHidden: Bool = true {
        didSet {
            updateActivityIndicator()
        }
    }
    
    var tapCallback: (() -> Void)?
    
    private let rippleController = MDCRippleTouchController()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setup()
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        let padding = frame.size.width * 0.2
        leftConstraint.constant = padding
        rightConstraint.constant = padding
        topConstraint.constant = padding
        bottomConstraint.constant = padding
    }
    
    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        if superview is PadView {
            return true
        } else {
            return super.point(inside: point, with: event)
        }
    }
    
    private func setup() {
        Bundle(for: ImageButton.self).loadNibNamed("ImageButton", owner: self, options: nil)
        addSubview(contentView)
        
        contentView.frame = self.bounds
        contentView.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        
        layer.borderWidth = 2
        layer.borderColor = UIColor.white.cgColor
        updateCornerRadius()
        clipsToBounds = true
        
        imageView.tintColor = .white

        rippleController.addRipple(to: self)
        
        updateActivityIndicator()
        
        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(onTapped(gesture:))))
    }
    
    private func updateImage() {
        imageView.image = image
    }
    
    private func updateCornerRadius() {
        layer.cornerRadius = isCircular ? frame.width / 2 : 2
    }
    
    private func updateActivityIndicator() {
        if isActivityIndicatorHidden {
            activityIndicator.stopAnimating()
            activityIndicator.isHidden = true
            imageView.isHidden = false
        } else {
            activityIndicator.startAnimating()
            activityIndicator.isHidden = false
            imageView.isHidden = true
        }
    }
    
    @objc private func onTapped(gesture: UIGestureRecognizer) {
        tapCallback?()
    }
}
