package dev.polek.episodetracker.common.presentation.showdetails.presenter

import dev.polek.episodetracker.common.analytics.Analytics
import dev.polek.episodetracker.common.analytics.Analytics.Screen
import dev.polek.episodetracker.common.datasource.db.QueryListener.Subscriber
import dev.polek.episodetracker.common.datasource.themoviedb.TmdbService
import dev.polek.episodetracker.common.datasource.themoviedb.TmdbService.Companion.backdropImageUrl
import dev.polek.episodetracker.common.datasource.themoviedb.entities.ShowDetailsEntity
import dev.polek.episodetracker.common.di.Inject
import dev.polek.episodetracker.common.logging.log
import dev.polek.episodetracker.common.logging.loge
import dev.polek.episodetracker.common.model.ContentRating
import dev.polek.episodetracker.common.model.EpisodeNumber
import dev.polek.episodetracker.common.model.Season
import dev.polek.episodetracker.common.preferences.Preferences
import dev.polek.episodetracker.common.presentation.BasePresenter
import dev.polek.episodetracker.common.presentation.showdetails.model.*
import dev.polek.episodetracker.common.presentation.showdetails.view.ShowDetailsView
import dev.polek.episodetracker.common.repositories.EpisodesRepository
import dev.polek.episodetracker.common.repositories.MyShowsRepository
import dev.polek.episodetracker.common.repositories.ShowRepository
import dev.polek.episodetracker.common.utils.string
import dev.polek.episodetracker.db.ShowDetails
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShowDetailsPresenter(
    private val showId: Int,
    private val myShowsRepository: MyShowsRepository,
    private val showRepository: ShowRepository,
    private val episodesRepository: EpisodesRepository,
    private val preferences: Preferences,
    private val analytics: Analytics) : BasePresenter<ShowDetailsView>()
{
    private var showDetails: ShowDetailsEntity? = null
    private var showSeasons: List<Season>? = null
    private var seasonsViewModel: SeasonsViewModel = SeasonsViewModel(emptyList())
    private var episodesTabRevealed = false

    private val isShowAddedOrAddingSubscriber = object : Subscriber<Boolean> {
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun onQueryResult(isAddedOrAdding: Boolean) {
            if (isAddedOrAdding) {
                view?.displayAddToMyShowsProgress()
                launch {
                    delay(500)
                    view?.hideAddToMyShowsButton()
                }
            } else {
                view?.displayAddToMyShowsButton()
            }
        }
    }

    private val isArchivedSubscriber = object : Subscriber<Boolean> {
        @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
        override fun onQueryResult(isArchived: Boolean) {
            if (isArchived) {
                view?.displayArchivedBadge()
            } else {
                view?.hideArchivedBadge()
            }
        }
    }

    override fun attachView(view: ShowDetailsView) {
        super.attachView(view)

        loadShowDetails()
    }

    override fun onViewAppeared() {
        super.onViewAppeared()
        myShowsRepository.setIsAddedOrAddingToMyShowsSubscriber(showId, isShowAddedOrAddingSubscriber)
        myShowsRepository.setIsArchivedSubscriber(showId, isArchivedSubscriber)

        showDetails?.let(::displayAdditionalInfo)
    }

    override fun onViewDisappeared() {
        myShowsRepository.removeIsAddedOrAddingToMyShowsSubscriber(showId)
        myShowsRepository.removeIsArchivedSubscriber(showId)
        super.onViewDisappeared()
    }

    fun onEpisodesTabSelected() {
        if (episodesTabRevealed) return

        episodesTabRevealed = true

        val inDb = myShowsRepository.isAddedToMyShows(showId)
        if (inDb) {
            loadEpisodesFromDb()
        } else {
            if (showDetails != null) {
                loadEpisodesFromNetwork()
            } else {
                view?.hideEpisodesProgress()
                view?.showEpisodesError()
            }
        }
    }

    fun onMenuClicked() {
        val isAddedOrAdding = myShowsRepository.isAddedOrAddingToMyShows(showId)
        val isArchived = myShowsRepository.isArchived(showId)
        view?.displayOptionsMenu(isInMyShows = isAddedOrAdding, isArchived = isArchived)
    }

    fun onAddToMyShowsClicked() {
        view?.displayAddToMyShowsProgress()
        launch {
            myShowsRepository.addShow(showId)
        }
        analytics.logAddShow(showId, Screen.SHOW_DETAILS)
    }

    fun onShareShowClicked() {
        val inDb = myShowsRepository.isAddedToMyShows(showId)
        val text = if (inDb) {
            val show = myShowsRepository.showDetails(showId) ?: return
            buildShareText(
                name = show.name,
                year = show.year,
                imdbId = show.imdbId,
                homePageUrl = show.homePageUrl)
        } else {
            val show = showDetails ?: return
            buildShareText(
                name = show.name.orEmpty(),
                year = show.year,
                imdbId = show.externalIds?.imdbId,
                homePageUrl = show.homePageUrl)
        }

        view?.shareText(text)
        analytics.logShare(text, Screen.SHOW_DETAILS)
    }

    fun onMarkWatchedClicked() {
        val inDb = myShowsRepository.isAddedToMyShows(showId)
        if (inDb) {
            episodesRepository.markAllWatched(showId)
        } else {
            myShowsRepository.removeShow(showId)
            myShowsRepository.addShow(showId, markAllEpisodesWatched = true)
        }
    }

    fun onRemoveShowClicked() {
        view?.displayRemoveConfirmation { confirmed ->
            if (!confirmed) return@displayRemoveConfirmation
            myShowsRepository.removeShow(showId)
            clearWatchedState()
        }
    }

    fun onArchiveShowClicked() {
        val inDb = myShowsRepository.isAddedToMyShows(showId)
        if (inDb) {
            myShowsRepository.archiveShow(showId)
        } else {
            myShowsRepository.removeShow(showId)
            myShowsRepository.addShow(showId, archive = true)
        }

        analytics.logArchiveShow(showId, Screen.SHOW_DETAILS)
    }

    fun onUnarchiveShowClicked() {
        val inDb = myShowsRepository.isAddedToMyShows(showId)
        if (inDb) {
            myShowsRepository.unarchiveShow(showId)
        } else {
            myShowsRepository.removeShow(showId)
            myShowsRepository.addShow(showId, archive = false)
        }

        analytics.logUnarchiveShow(showId, Screen.SHOW_DETAILS)
    }

    fun onRetryButtonClicked() {
        loadShowDetails()
    }

    fun onRecommendationClicked(recommendation: RecommendationViewModel) {
        view?.openRecommendation(recommendation)
        analytics.logOpenRecommendation(recommendation.showId)
    }

    fun onEpisodeWatchedStateToggled(episode: EpisodeViewModel) {
        val inDb = myShowsRepository.isAddedToMyShows(showId)

        if (inDb) {
            processEpisodeWatchedStateToggle(episode)
        } else {
            val show = checkNotNull(showDetails) { "`showDetails` must not be null at this moment" }
            val seasons = checkNotNull(showSeasons) { "`showSeasons` must not be null at this moment" }
            
            view?.displayAddToMyShowsConfirmation(showName = show.name.orEmpty()) { confirmed ->
                if (confirmed) {
                    showRepository.writeShowToDb(show, seasons)
                    processEpisodeWatchedStateToggle(episode)
                } else {
                    view?.reloadSeason(episode.number.season)
                }
            }
        }
    }

    fun onSeasonWatchedStateToggled(season: SeasonViewModel) {
        val inDb = myShowsRepository.isAddedToMyShows(showId)

        if (inDb) {
            processSeasonWatchedStateToggled(season)
        } else {
            val show = checkNotNull(showDetails) { "`showDetails` must not be null at this moment" }
            val seasons = checkNotNull(showSeasons) { "`showSeasons` must not be null at this moment" }

            view?.displayAddToMyShowsConfirmation(showName = show.name.orEmpty()) { confirmed ->
                if (confirmed) {
                    showRepository.writeShowToDb(show, seasons)
                    processSeasonWatchedStateToggled(season)
                } else {
                    view?.reloadSeason(season.number)
                }
            }
        }
    }

    fun onEpisodesRetryButtonClicked() {
        if (showDetails != null) {
            loadEpisodesFromNetwork()
        } else {
            loadShowDetails()
        }
    }

    private fun loadShowDetails() {
        view?.showProgress()
        view?.hideError()

        val inDb = myShowsRepository.isAddedToMyShows(showId)
        if (inDb) {
            val show = myShowsRepository.showDetails(showId)
            checkNotNull(show) { "Can't find show with $showId ID in My Shows" }
            displayHeader(show)
            displayDetails(show)
            loadEpisodesFromDb()
            view?.hideProgress()
        } else {
            view?.showEpisodesProgress()
        }

        launch {
            try {
                val show = showRepository.showDetails(showId)
                showDetails = show
                if (!inDb) {
                    displayHeader(show)
                    displayDetails(show)
                }
                displayAdditionalInfo(show)

                val imdbId = show.externalIds?.imdbId
                if (imdbId != null) {
                    loadImdbRating(imdbId)
                }
            } catch (e: Throwable) {
                if (!inDb) {
                    view?.showError()
                }
            } finally {
                view?.hideProgress()
            }

            if (episodesTabRevealed) {
                if (!inDb) {
                    if (showDetails != null) {
                        loadEpisodesFromNetwork()
                    } else {
                        view?.hideEpisodesProgress()
                        view?.showEpisodesError()
                    }
                }
            }
        }
    }

    fun onRefreshRequested() {
        val inDb = myShowsRepository.isAddedToMyShows(showId)
        if (inDb) {
            launch {
                showRepository.refreshShow(showId)
                loadShowDetails()
                view?.hideRefreshProgress()
            }
        } else {
            loadEpisodesFromNetwork(
                noCache = true,
                showProgress = {},
                hideProgress = { view?.hideRefreshProgress() })
        }
    }

    fun onContentRatingClicked() {
        val ratingName = showDetails?.contentRating ?: return
        val rating = ContentRating.find(ratingName) ?: return
        view?.displayContentRatingInfo(rating.abbr, string(rating.info))
    }

    fun onAddRecommendationClicked(show: RecommendationViewModel) {
        show.isAddInProgress = true
        view?.updateRecommendation(show)

        launch {
            myShowsRepository.addShow(show.showId)
            delay(300)
            show.isInMyShows = true
            show.isAddInProgress = false
            view?.updateRecommendation(show)
        }

        analytics.logAddRecommendation(show.showId)
    }

    fun onRemoveRecommendationClicked(show: RecommendationViewModel) {
        view?.displayRemoveRecommendationConfirmation(show) { confirmed ->
            if (!confirmed) return@displayRemoveRecommendationConfirmation

            show.isAddInProgress = true
            view?.updateRecommendation(show)

            launch {
                myShowsRepository.removeShow(show.showId)
                delay(300)
                show.isInMyShows = false
                show.isAddInProgress = false
                view?.updateRecommendation(show)
            }
        }

        analytics.logRemoveRecommendation(show.showId)
    }

    private fun displayHeader(show: ShowDetails) {
        val headerViewModel = ShowHeaderViewModel(
            name = show.name,
            imageUrl = show.imageUrl,
            rating = show.contentRating.orEmpty(),
            year = show.year,
            endYear = if (show.isEnded) show.lastYear else null,
            networks = show.networks)
        view?.displayShowHeader(headerViewModel)
    }

    private fun displayHeader(show: ShowDetailsEntity) {
        val headerViewModel = ShowHeaderViewModel(
            name = show.name.orEmpty(),
            imageUrl = show.backdropPath?.let(::backdropImageUrl),
            rating = show.contentRating.orEmpty(),
            year = show.year,
            endYear = if (show.isEnded) show.lastYear else null,
            networks = show.networks
        )
        view?.displayShowHeader(headerViewModel)
    }

    private fun displayDetails(show: ShowDetails) {
        val detailsViewModel = ShowDetailsViewModel(
            id = show.tmdbId,
            overview = show.overview,
            genres = show.genres,
            homePageUrl = show.homePageUrl,
            imdbId = show.imdbId,
            instagramUsername = show.instagramId,
            facebookProfile = show.facebookId,
            twitterUsername = show.twitterId)
        view?.displayShowDetails(detailsViewModel)
    }

    private fun displayDetails(show: ShowDetailsEntity) {
        val detailsViewModel = ShowDetailsViewModel(
            id = show.tmdbId!!,
            overview = show.overview.orEmpty(),
            genres = show.genres,
            homePageUrl = show.homePageUrl,
            imdbId = show.externalIds?.imdbId,
            instagramUsername = show.externalIds?.instagramId,
            facebookProfile = show.externalIds?.facebookId,
            twitterUsername = show.externalIds?.twitterId)
        view?.displayShowDetails(detailsViewModel)
    }

    private fun displayAdditionalInfo(show: ShowDetailsEntity) {
        val trailers = show.videos.map { video ->
            TrailerViewModel(
                name = video.name.orEmpty(),
                youtubeKey = video.key.orEmpty())
        }
        val castMembers = show.cast.map { member ->
            CastMemberViewModel(
                name = member.name.orEmpty(),
                character = member.character.orEmpty(),
                portraitImageUrl = member.profilePath?.let(TmdbService.Companion::profileImageUrl))
        }
        val recommendations = show.recommendations.map { recommendation ->
            RecommendationViewModel(
                showId = recommendation.tmdbId!!,
                name = recommendation.name.orEmpty(),
                imageUrl = recommendation.backdropPath?.let(::backdropImageUrl),
                year = recommendation.year,
                networks = recommendation.networks,
                isInMyShows = myShowsRepository.isAddedOrAddingToMyShows(recommendation.tmdbId))
        }

        view?.displayTrailers(trailers)
        view?.displayCast(castMembers)
        view?.displayRecommendations(recommendations)
    }

    private suspend fun loadImdbRating(imdbId: String) {
        launch {
            try {
                val imdbRating = showRepository.imdbRating(imdbId)
                if (imdbRating != null) {
                    view?.displayImdbRating(imdbRating)
                }
            } catch (e: Throwable) {
                log { "Failed to load IMDB rating: $e" }
            }
        }
    }

    private fun loadEpisodesFromDb() {
        showSeasons = episodesRepository.allSeasons(showId)

        val showSpecials = preferences.showSpecials
        val seasonsList = showSeasons!!.filter { season -> showSpecials || season.number > 0 }
            .map(SeasonViewModel.Companion::map)
        val oldSeasonsViewModel = seasonsViewModel
        seasonsViewModel = SeasonsViewModel(seasonsList)
        oldSeasonsViewModel.asList().forEach { season ->
            seasonsViewModel[season.number]?.isExpanded = season.isExpanded
        }
        view?.displayEpisodes(seasonsList)
        view?.hideEpisodesProgress()
    }

    private fun loadEpisodesFromNetwork(
        noCache: Boolean = false,
        showProgress: () -> Unit = { view?.showEpisodesProgress() },
        hideProgress: () -> Unit = { view?.hideEpisodesProgress() })
    {
        val seasonNumbers = showDetails?.seasonNumbers
        if (seasonNumbers == null) {
            loge { "Can't load episodes list without ShowDetails" }
            return
        }

        showProgress()
        view?.hideEpisodesError()

        launch {
            try {
                val showSpecials = preferences.showSpecials
                showSeasons = seasonNumbers
                    .filter { season -> showSpecials || season > 0 }
                    .mapNotNull { seasonNumber ->
                        log { "map season #$seasonNumber" }
                        showRepository.season(showTmdbId = showId, seasonNumber = seasonNumber, noCache = noCache)
                    }
                val seasonsList = showSeasons!!.map(SeasonViewModel.Companion::map)
                seasonsViewModel = SeasonsViewModel(seasonsList)
                view?.displayEpisodes(seasonsList)
            } catch (error: Throwable) {
                loge { "loadEpisodesFromNetwork: $error" }
                view?.showEpisodesError()
            } finally {
                hideProgress()
            }
        }
    }

    private fun processEpisodeWatchedStateToggle(episode: EpisodeViewModel) {
        val isWatched = !episode.isWatched

        val firstNotWatchedNumber = if (episode.isSpecial) {
            episodesRepository.firstNotWatchedSpecialEpisode(showId)
        } else {
            episodesRepository.firstNotWatchedEpisode(showId)
        }
        if (isWatched && firstNotWatchedNumber != null && firstNotWatchedNumber < episode.number) {
            view?.showCheckAllPreviousEpisodesPrompt(
                onCheckAllPrevious = {
                    if (episode.isSpecial) {
                        markAllSpecialEpisodesWatchedUpTo(episode.number)
                    } else {
                        markAllEpisodesWatchedUpTo(episode.number)
                    }
                },
                onCheckOnlyThis = {
                    setEpisodeWatched(episode, isWatched)
                },
                onCancel = {
                    view?.reloadSeason(episode.number.season)
                }
            )
        } else {
            setEpisodeWatched(episode, isWatched)
        }
    }

    private fun processSeasonWatchedStateToggled(season: SeasonViewModel) {
        val isWatched = !season.isWatched

        val firstNotWatchedSeason = episodesRepository.firstNotWatchedEpisode(showId)?.season ?: season.number
        if (isWatched && firstNotWatchedSeason < season.number) {
            view?.showCheckAllPreviousEpisodesPrompt(
                onCheckAllPrevious = {
                    markAllSeasonsWatchedUpTo(season.number)
                },
                onCheckOnlyThis = {
                    setSeasonWatched(season, isWatched)
                },
                onCancel = {
                    view?.reloadSeason(season.number)
                }
            )
        } else {
            setSeasonWatched(season, isWatched)
        }
    }

    private fun setEpisodeWatched(episode: EpisodeViewModel, isWatched: Boolean) {
        episode.isWatched = isWatched

        episodesRepository.setEpisodeWatched(
            showTmdbId = showId,
            seasonNumber = episode.number.season,
            episodeNumber = episode.number.episode,
            isWatched = isWatched)

        view?.reloadSeason(episode.number.season)
    }

    private fun setSeasonWatched(season: SeasonViewModel, isWatched: Boolean) {
        season.isWatched = isWatched

        episodesRepository.setSeasonWatched(
            showTmdbId = showId,
            seasonNumber = season.number,
            isWatched = isWatched)

        view?.reloadSeason(season.number)
    }

    private fun markAllEpisodesWatchedUpTo(episodeNumber: EpisodeNumber) {
        seasonsViewModel.get(1 until episodeNumber.season).forEach { season ->
            season.isWatched = true
        }

        seasonsViewModel[episodeNumber.season]?.episodes?.toEpisodes()?.get(1..episodeNumber.episode)?.forEach { episode ->
            episode.isWatched = true
        }

        episodesRepository.markAllWatchedUpTo(episodeNumber = episodeNumber, showTmdbId = showId)

        (1..episodeNumber.season).forEach { season ->
            view?.reloadSeason(season)
        }
    }

    private fun markAllSpecialEpisodesWatchedUpTo(episodeNumber: EpisodeNumber) {
        seasonsViewModel[episodeNumber.season]?.episodes?.toEpisodes()?.get(1..episodeNumber.episode)?.forEach { episode ->
            episode.isWatched = true
        }
        episodesRepository.markAllWatchedUpTo(episodeNumber = episodeNumber, showTmdbId = showId)
        view?.reloadSeason(episodeNumber.season)
    }

    private fun markAllSeasonsWatchedUpTo(seasonNumber: Int) {
        seasonsViewModel.get(1..seasonNumber).forEach { season ->
            season.isWatched = true
        }

        episodesRepository.markAllWatchedUpToSeason(season = seasonNumber, showTmdbId = showId)

        (1..seasonNumber).forEach { season ->
            view?.reloadSeason(season)
        }
    }

    private fun clearWatchedState() {
        seasonsViewModel.asList().forEach { season ->
            season.isWatched = false
            view?.reloadSeason(season.number)
        }
    }

    companion object {
        private fun buildShareText(name: String, year: Int?, imdbId: String?, homePageUrl: String?): String {
            return buildString {
                append(name)
                if (year != null) append(" ($year)")
                if (imdbId != null) {
                    val imdbUrl = imdbId.let { "https://www.imdb.com/title/$it" }
                    append("\n$imdbUrl")
                } else if (homePageUrl != null) {
                    append("\n$homePageUrl")
                }
            }
        }
    }

    class Factory @Inject constructor(
        private val myShowsRepository: MyShowsRepository,
        private val showRepository: ShowRepository,
        private val episodesRepository: EpisodesRepository,
        private val preferences: Preferences,
        private val analytics: Analytics)
    {
        fun create(showId: Int): ShowDetailsPresenter {
            return ShowDetailsPresenter(showId, myShowsRepository, showRepository, episodesRepository, preferences, analytics)
        }
    }
}
