import UIKit
import SharedCode

class PersonDetailsViewController: UIViewController {
    
    static func instantiate(personId: Int) -> PersonDetailsViewController {
        let storyboard = UIStoryboard(name: "PersonDetails", bundle: Bundle.main)
        let vc = storyboard.instantiateInitialViewController() as! PersonDetailsViewController
        vc.personId = personId
        return vc
    }
    
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var birthdayLabel: UILabel!
    @IBOutlet weak var birthPlaceLabel: UILabel!
    @IBOutlet weak var imageView: ImageView!
    @IBOutlet weak var biographyLabel: UILabel!
    @IBOutlet weak var imdbBadge: ImdbBadge!
    @IBOutlet weak var homePageButton: IconButton!
    @IBOutlet weak var instagramButton: IconButton!
    @IBOutlet weak var facebookButton: IconButton!
    @IBOutlet weak var twitterButton: IconButton!
    @IBOutlet weak var showsCollectionView: UICollectionView!
    @IBOutlet weak var showsContainer: UIView!
    
    private var personId: Int!
    private var presenter: PersonDetailsPresenter!
    
    private let showsDataSource = RecommendationsDataSource()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        showsCollectionView.dataSource = showsDataSource
        showsCollectionView.delegate = self
        
        presenter = PersonDetailsPresenter(personId: personId.int32, tmdbService: AppDelegate.instance().tmdbService)
        presenter.attachView(view: self)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        presenter.onViewAppeared()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        presenter.onViewDisappeared()
    }
}

// MARK: - PersonDetailsView implementation
extension PersonDetailsViewController: PersonDetailsView {
    
    func displayPersonDetails(person: PersonViewModel) {
        nameLabel.text = person.name
        imageView.imageUrl = person.imageUrl
        birthdayLabel.text = person.dates
        birthPlaceLabel.text = person.birthPlace
        biographyLabel.text = person.biography
        
        imdbBadge.isHidden = person.imdbUrl == nil
        imdbBadge.tapCallback = {
            person.imdbUrl?.toUrl()?.open()
        }
        
        homePageButton.isHidden = person.homePageUrl == nil
        homePageButton.tapCallback = {
            person.homePageUrl?.toUrl()?.open()
        }
        
        instagramButton.isHidden = person.instagramUsername == nil
        instagramButton.tapCallback = {
            let appUrl = "instagram://user?username=\(person.instagramUsername!)".toUrl()!
            if appUrl.canBeOpen() {
                appUrl.open()
            } else {
                person.instagramUrl?.toUrl()?.open()
            }
        }
        
        facebookButton.isHidden = person.facebookProfile == nil
        facebookButton.tapCallback = {
            person.facebookUrl?.toUrl()?.open()
        }
        
        twitterButton.isHidden = person.twitterUsername == nil
        twitterButton.tapCallback = {
            let appUrl = "twitter://user?screen_name=\(person.twitterUsername!)".toUrl()!
            if appUrl.canBeOpen() {
                appUrl.open()
            } else {
                person.twitterUrl?.toUrl()?.open()
            }
        }
        
        showsDataSource.recommendations = person.shows
        showsCollectionView.reloadData()
        showsContainer.isHidden = person.shows.isEmpty
    }
    
    func openShow(show: RecommendationViewModel) {
        let vc = ShowDetailsViewController.instantiate(showId: show.showId.int, showName: show.name)
        navigationController?.pushViewController(vc, animated: true)
    }
}

// MARK: - UICollectionViews delegate
extension PersonDetailsViewController: UICollectionViewDelegate {
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let show = showsDataSource.recommendations[indexPath.row]
        presenter.onShowClicked(show: show)
    }
}