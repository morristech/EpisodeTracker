import UIKit
import MaterialComponents.MaterialTabs
import MaterialComponents.MaterialButtons
import SharedCode

class ShowDetailsViewController: UIViewController {
    
    static func instantiate(
        showId: Int,
        showName: String,
        openEpisodesTabOnStart: Bool = false,
        scrollToEpisodeOnStart: EpisodeNumber? = nil) -> ShowDetailsViewController
    {
        let storyboard = UIStoryboard(name: "ShowDetails", bundle: Bundle.main)
        let vc = storyboard.instantiateInitialViewController() as! ShowDetailsViewController
        vc.setParameters(Int(showId), showName, openEpisodesTabOnStart, scrollToEpisodeOnStart)
        return vc
    }
    
    private var showId: Int!
    private var showName: String!
    private var openEpisodesTabOnStart: Bool!
    private var scrollToEpisodeOnStart: EpisodeNumber?
    private var presenter: ShowDetailsPresenter!
    private weak var aboutShowViewController: AboutShowViewController?
    private weak var episodesViewController: EpisodesViewController?
    
    private let minHeaderHeight: CGFloat = 44 + UIApplication.shared.statusBarFrame.height
    private var maxHeaderHeight: CGFloat!
    
    @IBOutlet weak var contentView: UIView!
    @IBOutlet weak var activityIndicator: UIActivityIndicatorView!
    @IBOutlet weak var errorView: ErrorView!
    @IBOutlet weak var toolbar: UIView!
    @IBOutlet weak var imageView: ImageView!
    @IBOutlet weak var imageViewHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var headerLabelsContainer: UIView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var subheadLabel: UILabel!
    @IBOutlet weak var ratingButton: UIButton!
    @IBOutlet weak var tabBar: MDCTabBar!
    @IBOutlet weak var aboutView: UIView!
    @IBOutlet weak var episodesView: UIView!
    @IBOutlet weak var backButton: UIButton!
    @IBOutlet weak var menuButton: UIButton!
    @IBOutlet weak var addButton: FloatingButton!
    @IBOutlet weak var addButtonBottomConstraint: NSLayoutConstraint!
    @IBOutlet weak var toolbarLabel: UILabel!
    @IBOutlet weak var archivedBadge: UIView!
    
    private func setParameters(
        _ showId: Int,
        _ showName: String,
        _ openEpisodesTabOnStart: Bool,
        _ scrollToEpisodeOnStart: EpisodeNumber? = nil)
    {
        self.showId = showId
        self.showName = showName
        self.openEpisodesTabOnStart = openEpisodesTabOnStart
        self.scrollToEpisodeOnStart = scrollToEpisodeOnStart
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        maxHeaderHeight = imageView.bounds.width / 1.78
        imageViewHeightConstraint.constant = maxHeaderHeight
        imageView.overlayOpacity = [0.6, 0.4, 0.4, 0.6]
        imageView.isBlured = true
        imageView.blurAlpha = 0
        
        toolbarLabel.text = showName
        toolbarLabel.alpha = 0
        headerLabelsContainer.alpha = 1
        archivedBadge.alpha = 1
        
        tabBar.items = [
            UITabBarItem(title: string(R.str.tab_about), image: nil, tag: 0),
            UITabBarItem(title: string(R.str.tab_episodes), image: nil, tag: 1)
        ]
        tabBar.itemAppearance = .titles
        tabBar.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
        tabBar.sizeToFit()
        tabBar.bottomDividerColor = .dividerPrimary
        tabBar.setTitleColor(.textColorSecondary, for: .normal)
        tabBar.setTitleColor(.textColorPrimary, for: .selected)
        tabBar.selectedItemTitleFont = .systemFont(ofSize: 17)
        tabBar.unselectedItemTitleFont = .systemFont(ofSize: 17)
        tabBar.displaysUppercaseTitles = false
        tabBar.tintColor = .accent
        tabBar.inkColor = .transparent
        tabBar.alignment = .justified
        tabBar.delegate = self
        
        addButton.mode = .expanded
        
        if let bottomInset = UIApplication.shared.keyWindow?.safeAreaInsets.bottom {
            addButtonBottomConstraint.constant = bottomInset > 0 ? 0 : 16
        }
        
        let app = AppDelegate.instance()
        presenter = ShowDetailsPresenter(
            showId: Int32(showId),
            myShowsRepository: app.myShowsRepository,
            showRepository: app.showRepository,
            episodesRepository: app.episodesRepository,
            preferences: app.preferences,
            analytics: app.analytics)
        presenter.attachView(view: self)
        
        if openEpisodesTabOnStart {
            showEpisodesTab()
        } else {
            showAboutTab()
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        aboutShowViewController?.trailerTapCallback = trailerTapCallback(trailer:)
        aboutShowViewController?.castMemberTapCallback = castMemberTapCallback(castMember:)
        aboutShowViewController?.recommendationTapCallback = recommendationTapCallback(recommendation:)
        aboutShowViewController?.recommendationAddCallback = recommendationAddCallback(recommendation:)
        aboutShowViewController?.recommendationRemoveCallback = recommendationRemoveCallback(recommendation:)
        aboutShowViewController?.refreshRequestedCallback = refreshRequestedCallback
        aboutShowViewController?.scrollCallback = scrollCallback(offset:)
        
        episodesViewController?.seasonWatchedStateToggleCallback = seasonWatchedStateToggleCallback(season:)
        episodesViewController?.episodeWatchedStateToggleCallback = episodeWatchedStateToggleCallback(episode:)
        episodesViewController?.retryTapCallback = episodesRetryTapCallback
        episodesViewController?.refreshRequestedCallback = refreshRequestedCallback
        episodesViewController?.scrollCallback = scrollCallback(offset:)
        
        errorView.retryTappedCallback = { [weak self] in
            self?.presenter.onRetryButtonClicked()
        }
        
        presenter.onViewAppeared()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        aboutShowViewController?.trailerTapCallback = nil
        aboutShowViewController?.castMemberTapCallback = nil
        aboutShowViewController?.recommendationTapCallback = nil
        aboutShowViewController?.recommendationAddCallback = nil
        aboutShowViewController?.recommendationRemoveCallback = nil
        aboutShowViewController?.refreshRequestedCallback = nil
        aboutShowViewController?.scrollCallback = nil
        
        episodesViewController?.seasonWatchedStateToggleCallback = nil
        episodesViewController?.episodeWatchedStateToggleCallback = nil
        episodesViewController?.retryTapCallback = nil
        episodesViewController?.refreshRequestedCallback = nil
        episodesViewController?.scrollCallback = nil
        
        presenter.onViewDisappeared()
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        get { .lightContent }
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        switch segue.identifier {
        case "about_view":
            aboutShowViewController = (segue.destination as! AboutShowViewController)
            aboutShowViewController!.showId = showId
            break
        case "episodes_view":
            episodesViewController = (segue.destination as! EpisodesViewController)
            episodesViewController!.showId = showId
            episodesViewController!.scrollToEpisodeOnStart = scrollToEpisodeOnStart
            break
        default:
            break
        }
    }
    
    @IBAction func onBackTapped(_ sender: Any) {
        navigationController?.popViewController(animated: true)
    }
    
    @IBAction func onMenuTapped(_ sender: Any) {
        presenter.onMenuClicked()
    }
    
    @IBAction func onAddToMyShowsTapped(_ sender: Any) {
        presenter.onAddToMyShowsClicked()
    }
    
    @IBAction func onArchivedButtonTapped(_ sender: Any) {
        let alert = UIAlertController(title: string(R.str.unarchive_show_confirmation_title), message: string(R.str.unarchive_show_confirmation_message), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: string(R.str.action_unarchive), style: .default, handler: { action in
            self.presenter.onUnarchiveShowClicked()
        }))
        alert.addAction(UIAlertAction(title: string(R.str.action_cancel), style: .cancel, handler: nil))
        present(alert, animated: true, completion: nil)
    }
    
    @IBAction func onRatingButtonTapped(_ sender: Any) {
        presenter.onContentRatingClicked()
    }
    
    private func trailerTapCallback(trailer: TrailerViewModel) {
        var url: URL = URL(string: "youtube://\(trailer.youtubeKey)")!
        if !url.canBeOpen() {
            url = URL(string: trailer.url)!
        }
        url.open()
    }
    
    private func castMemberTapCallback(castMember: CastMemberViewModel) {
        castMember.wikipediaUrl.toUrl()?.open()
    }
    
    private func recommendationTapCallback(recommendation: RecommendationViewModel) {
        presenter.onRecommendationClicked(recommendation: recommendation)
    }
    
    private func recommendationAddCallback(recommendation: RecommendationViewModel) {
        presenter.onAddRecommendationClicked(show: recommendation)
    }
    
    private func recommendationRemoveCallback(recommendation: RecommendationViewModel) {
        presenter.onRemoveRecommendationClicked(show: recommendation)
    }
    
    private func seasonWatchedStateToggleCallback(season: SeasonViewModel) {
        presenter.onSeasonWatchedStateToggled(season: season)
    }
    
    private func episodeWatchedStateToggleCallback(episode: EpisodeViewModel) {
        presenter.onEpisodeWatchedStateToggled(episode: episode)
    }
    
    private func episodesRetryTapCallback() {
        presenter.onEpisodesRetryButtonClicked()
    }
    
    private func refreshRequestedCallback() {
        presenter.onRefreshRequested()
    }
    
    private func scrollCallback(offset: CGFloat) -> Bool {
        var newHeight = imageViewHeightConstraint.constant - offset
        var blockScroll = false
        
        if newHeight > maxHeaderHeight {
            newHeight = maxHeaderHeight
        } else if newHeight < minHeaderHeight {
            newHeight = minHeaderHeight
        } else {
            blockScroll = true
        }
        imageViewHeightConstraint.constant = newHeight
        
        let heightRatio = (newHeight - minHeaderHeight) / (maxHeaderHeight - minHeaderHeight)
        toolbarLabel.alpha = 1 - 2 * heightRatio
        headerLabelsContainer.alpha = 1 - 2 * (1 - heightRatio)
        imageView.blurAlpha = 1 - heightRatio
        archivedBadge.alpha = 1 - 2 * (1 - heightRatio)

        return blockScroll
    }
    
    private func showAboutTab() {
        tabBar.setSelectedItem(tabBar.items[0], animated: false)
        aboutView.isHidden = false
        episodesView.isHidden = true
    }
    
    private func showEpisodesTab() {
        presenter.onEpisodesTabSelected()
        tabBar.setSelectedItem(tabBar.items[1], animated: false)
        episodesView.isHidden = false
        aboutView.isHidden = true
    }
    
    private func setBottomInset() {
        let inset = addButton.bounds.height + addButtonBottomConstraint.constant
        aboutShowViewController?.setBottomInset(inset)
        episodesViewController?.setBottomInset(inset)
    }
    
    private func removeBottomInset() {
        aboutShowViewController?.setBottomInset(0)
        episodesViewController?.setBottomInset(0)
    }
    
    private func setToolbarTextColor(_ color: UIColor) {
        toolbarLabel.textColor = color
        backButton.imageView?.tintColor = color
        menuButton.imageView?.tintColor = color
    }
}

// MARK: - ShowDetailsView implementation
extension ShowDetailsViewController: ShowDetailsView {
    
    func displayShowHeader(show: ShowHeaderViewModel) {
        nameLabel.text = show.name
        toolbarLabel.text = show.name
        imageView.imageUrl = show.imageUrl
        subheadLabel.text = show.subhead
        ratingButton.setTitle(show.rating, for: .normal)
        ratingButton.isHidden = show.rating.isEmpty
        contentView.isHidden = false
        setToolbarTextColor(.textColorPrimaryInverse)
    }
    
    func showProgress() {
        activityIndicator.startAnimating()
        contentView.isHidden = true
        setToolbarTextColor(.textColorPrimary)
    }
    
    func hideProgress() {
        activityIndicator.stopAnimating()
    }
    
    func showError() {
        errorView.isHidden = false
        contentView.isHidden = true
        setToolbarTextColor(.textColorPrimary)
        toolbarLabel.alpha = 1
    }
    
    func hideError() {
        errorView.isHidden = true
    }
    
    func displayAddToMyShowsButton() {
        addButton.isHidden = false
        addButton.isEnabled = true
        addButton.isActivityIndicatorHidden = true
        setBottomInset()
    }
    
    func displayAddToMyShowsProgress() {
        addButton.isActivityIndicatorHidden = false
        addButton.isEnabled = false
    }
    
    func hideAddToMyShowsButton() {
        addButton.isActivityIndicatorHidden = true
        addButton.isHidden = true
        removeBottomInset()
    }
    
    func displayArchivedBadge() {
        archivedBadge.isHidden = false
    }
    
    func hideArchivedBadge() {
        archivedBadge.isHidden = true
    }
    
    func displayAddToMyShowsConfirmation(showName: String, callback: @escaping (KotlinBoolean) -> Void) {
        let alert = UIAlertController(title: nil, message: string(R.str.episodes_add_to_my_shows_confirmation, showName), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: string(R.str.action_add), style: .default, handler: { action in
            callback(true)
        }))
        alert.addAction(UIAlertAction(title: string(R.str.action_cancel), style: .cancel, handler: { action in
            callback(false)
        }))
        present(alert, animated: true, completion: nil)
    }
    
    func displayOptionsMenu(isInMyShows: Bool, isArchived: Bool) {
        let alert = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        alert.addAction(UIAlertAction(title: string(R.str.action_share), style: .default, handler: { [weak self] action in
            self?.presenter.onShareShowClicked()
        }))
        
        alert.addAction(UIAlertAction(title: string(R.str.action_mark_watched), style: .default, handler: { [weak self] action in
            self?.presenter.onMarkWatchedClicked()
        }))
        
        if isInMyShows {
            if isArchived {
                alert.addAction(UIAlertAction(title: string(R.str.action_unarchive), style: .default, handler: { [weak self] action in
                    self?.presenter.onUnarchiveShowClicked()
                }))
            } else {
                alert.addAction(UIAlertAction(title: string(R.str.action_archive), style: .default, handler: { [weak self] action in
                    self?.presenter.onArchiveShowClicked()
                }))
            }
            
            alert.addAction(UIAlertAction(title: string(R.str.action_remove), style: .destructive, handler: { [weak self] action in
                self?.presenter.onRemoveShowClicked()
            }))
        } else {
            alert.addAction(UIAlertAction(title: string(R.str.action_add_to_my_shows), style: .default, handler: { [weak self] action in
                self?.presenter.onAddToMyShowsClicked()
            }))
        }
        
        alert.addAction(UIAlertAction(title: string(R.str.action_cancel), style: .cancel, handler: nil))
        
        present(alert, animated: true, completion: nil)
    }
    
    func displayRemoveConfirmation(callback: @escaping (KotlinBoolean) -> Void) {
        let alert = UIAlertController(title: nil, message: string(R.str.remove_show_confirmation, showName ?? ""), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: string(R.str.action_remove), style: .destructive, handler: { _ in callback(true)}))
        alert.addAction(UIAlertAction(title: string(R.str.action_cancel), style: .cancel, handler: { _ in callback(false) }))
        present(alert, animated: true, completion: nil)
    }
    
    func shareText(text: String) {
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        present(controller, animated: true, completion: nil)
    }
    
    func displayContentRatingInfo(rating: String, text: ResourcesStringDesc) {
        let alert = UIAlertController(title: rating, message: text.localized(), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: string(R.str.action_ok), style: .cancel, handler: nil))
        present(alert, animated: true, completion: nil)
    }
    
    func displayShowDetails(show: ShowDetailsViewModel) {
        aboutShowViewController?.displayShowDetails(show)
    }
    
    func displayTrailers(trailers: [TrailerViewModel]) {
        aboutShowViewController?.displayTrailers(trailers)
    }
    
    func displayCast(castMembers: [CastMemberViewModel]) {
        aboutShowViewController?.displayCast(castMembers)
    }
    
    func displayRecommendations(recommendations: [RecommendationViewModel]) {
        aboutShowViewController?.displayRecommendations(recommendations)
    }
    
    func updateRecommendation(show: RecommendationViewModel) {
        aboutShowViewController?.updateRecommendation(show)
    }
    
    func displayRemoveRecommendationConfirmation(show: RecommendationViewModel, callback: @escaping (KotlinBoolean) -> Void) {
        let alert = UIAlertController(title: nil, message: string(R.str.remove_show_confirmation, show.name), preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: string(R.str.action_remove), style: .destructive, handler: { _ in callback(true) }))
        alert.addAction(UIAlertAction(title: string(R.str.action_cancel), style: .cancel, handler: { _ in callback(false) }))
        present(alert, animated: true, completion: nil)
    }
    
    func openRecommendation(show: RecommendationViewModel) {
        aboutShowViewController?.openRecommendation(show)
    }
    
    func displayImdbRating(rating: Float) {
        aboutShowViewController?.displayImdbRating(rating)
    }
    
    func displayEpisodes(seasons: [SeasonViewModel]) {
        episodesViewController?.displaySeasons(seasons)
    }
    
    func reloadSeason(number: Int32) {
        episodesViewController?.reloadSeason(number)
    }
    
    func showCheckAllPreviousEpisodesPrompt(
        onCheckAllPrevious: @escaping () -> Void,
        onCheckOnlyThis: @escaping () -> Void,
        onCancel: @escaping () -> Void)
    {
        let alert = UIAlertController(title: string(R.str.episodes_check_previous_confirmation), message: nil, preferredStyle: .actionSheet)
        alert.addAction(UIAlertAction(title: string(R.str.action_check_all_previous), style: .default, handler: { _ in
            onCheckAllPrevious()
        }))
        alert.addAction(UIAlertAction(title: string(R.str.action_check_one), style: .default, handler: { _ in
            onCheckOnlyThis()
        }))
        alert.addAction(UIAlertAction(title: string(R.str.action_cancel), style: .cancel, handler: { _ in
            onCancel()
        }))
        present(alert, animated: true, completion: nil)
    }
    
    func showEpisodesProgress() {
        episodesViewController?.showActivityIndicator()
    }
    
    func hideEpisodesProgress() {
        episodesViewController?.hideActivityIndicator()
    }
    
    func showEpisodesError() {
        episodesViewController?.showError()
    }
    
    func hideEpisodesError() {
        episodesViewController?.hideError()
    }
    
    func hideRefreshProgress() {
        aboutShowViewController?.hideRefreshProgress()
        episodesViewController?.hideRefreshProgress()
    }
}

extension ShowDetailsViewController: MDCTabBarDelegate {
    
    func tabBar(_ tabBar: MDCTabBar, didSelect item: UITabBarItem) {
        switch item.tag {
        case 0:
            showAboutTab()
        case 1:
            showEpisodesTab()
        default:
            break
        }
    }
    
}
