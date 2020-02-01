import UIKit
import SharedCode

class ShowDetailsViewController: UIViewController {
    
    var showId: Int!
    var openEpisodesTabOnStart: Bool!
    var presenter: ShowDetailsPresenter!
    
    @IBOutlet weak var imageView: ImageView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var yearsLabel: UILabel!
    @IBOutlet weak var ratingLabel: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        if let navigationBar = navigationController?.navigationBar {
            navigationBar.setBackgroundImage(UIImage(), for: .default)
            navigationBar.shadowImage = UIImage()
            navigationBar.isTranslucent = true
        }
        
        imageView.overlayOpacity = [0.6, 0.2, 0.4, 0.6]
        
        presenter = ShowDetailsPresenter(showId: Int32(showId), repository: AppDelegate.instance().myShowsRepository)
        presenter.attachView(view: self)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        presenter.onViewAppeared()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        presenter.onViewDisappeared()
    }
    
    @IBAction func onBackTapped(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }
}

extension ShowDetailsViewController: ShowDetailsView {
    
    func displayShowDetails(show: ShowDetailsViewModel) {
        nameLabel.text = show.name
        imageView.imageUrl = show.imageUrl
        yearsLabel.text = show.years
        ratingLabel.text = show.contentRating
    }
    
    func close() {
        dismiss(animated: true, completion: nil)
    }
}