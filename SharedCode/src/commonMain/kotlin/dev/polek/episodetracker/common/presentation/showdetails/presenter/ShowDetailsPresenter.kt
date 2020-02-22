package dev.polek.episodetracker.common.presentation.showdetails.presenter

import dev.polek.episodetracker.common.logging.log
import dev.polek.episodetracker.common.presentation.BasePresenter
import dev.polek.episodetracker.common.presentation.showdetails.model.ShowHeaderViewModel
import dev.polek.episodetracker.common.presentation.showdetails.view.ShowDetailsView
import dev.polek.episodetracker.common.repositories.MyShowsRepository

class ShowDetailsPresenter(
    private val showId: Int,
    private val repository: MyShowsRepository) : BasePresenter<ShowDetailsView>()
{

    override fun attachView(view: ShowDetailsView) {
        super.attachView(view)
        loadShow()
    }

    private fun loadShow() {
        val show = repository.showDetails(showId)
        if (show == null) {
            view?.close()
            log("Can't find show with $showId ID in My Shows")
            return
        }

        val headerViewModel = ShowHeaderViewModel(
            name = show.name,
            imageUrl = show.imageUrl,
            rating = show.contentRating.orEmpty(),
            year = show.year,
            endYear = if (show.isEnded) show.lastYear else null,
            network = show.networkName)

        view?.displayShowHeader(headerViewModel)
    }
}
