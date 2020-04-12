package dev.polek.episodetracker.common.presentation.towatch

import co.touchlab.stately.ensureNeverFrozen
import dev.polek.episodetracker.common.datasource.db.QueryListener
import dev.polek.episodetracker.common.model.ToWatchShow
import dev.polek.episodetracker.common.presentation.BasePresenter
import dev.polek.episodetracker.common.repositories.EpisodesRepository
import dev.polek.episodetracker.common.repositories.ToWatchRepository

class ToWatchPresenter(
    private val toWatchRepository: ToWatchRepository,
    private val episodesRepository: EpisodesRepository) : BasePresenter<ToWatchView>(), QueryListener.Subscriber<List<ToWatchShow>>
{
    private var shows = emptyList<ToWatchShowViewModel>()
    private var searchQuery = ""
    private val isFiltered: Boolean
        get() = searchQuery.isNotEmpty() && shows.isNotEmpty()

    init {
        ensureNeverFrozen()
    }

    override fun onViewAppeared() {
        toWatchRepository.setToWatchShowsSubscriber(this)
    }

    override fun onViewDisappeared() {
        toWatchRepository.removeToWatchShowsSubscriber()
        super.onViewDisappeared()
    }

    fun onWatchedButtonClicked(show: ToWatchShowViewModel) {
        episodesRepository.markNextEpisodeWatched(showTmdbId = show.id)
    }

    fun onShowClicked(show: ToWatchShowViewModel) {
        view?.openToWatchShowDetails(show)
    }

    fun onMarkAllWatchedClicked(show: ToWatchShowViewModel) {
        episodesRepository.markAllWatched(show.id)
    }

    fun onSearchQueryChanged(text: String) {
        searchQuery = text.trim()
        displayShows()
    }

    override fun onQueryResult(result: List<ToWatchShow>) {
        shows = result.map(ToWatchShowViewModel.Companion::map)
        displayShows()
    }

    private fun displayShows() {
        val filteredShows = shows.filter { show ->
            show.name.contains(searchQuery, ignoreCase = true)
        }
        view?.displayShows(filteredShows)
        if (filteredShows.isEmpty()) {
            view?.showEmptyMessage(isFiltered)
        } else {
            view?.hideEmptyMessage()
        }
    }
}
