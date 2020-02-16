import UIKit
import MaterialComponents.MaterialRipple

class TrailerCell: RippleCollectionViewCell {
    
    @IBOutlet weak var previewImageView: ImageView!
    @IBOutlet weak var nameLabel: UILabel!
    
    override func layoutSubviews() {
        super.layoutSubviews()
        setup()
    }
    
    private func setup() {
        rippleView.layer.cornerRadius = previewImageView.layer.cornerRadius
    }
}
