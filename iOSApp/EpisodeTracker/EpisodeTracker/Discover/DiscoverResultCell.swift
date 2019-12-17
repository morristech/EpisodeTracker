import UIKit
import SharedCode

class DiscoverResultCell: UITableViewCell {
    
    @IBOutlet weak var posterView: ImageView!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var subtitleLabel: UILabel!
    @IBOutlet weak var overviewLabel: UILabel!
    @IBOutlet weak var divider: Divider!
    
    func bind(result: DiscoverResultViewModel) {
        posterView.imageUrl = result.posterUrl
        titleLabel.text = result.name
        overviewLabel.text = result.overview
        
        let yearText = String(result.year)
        let subtitleText = "\(yearText) |"
        subtitleLabel.attributedText = subtitleText.bold(font: subtitleLabel.font, location: 0, length: yearText.count)
    }
}
