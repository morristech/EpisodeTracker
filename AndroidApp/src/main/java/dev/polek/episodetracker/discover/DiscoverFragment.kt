package dev.polek.episodetracker.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dev.polek.episodetracker.App
import dev.polek.episodetracker.common.presentation.discover.DiscoverView
import dev.polek.episodetracker.common.presentation.discover.model.DiscoverResultViewModel
import dev.polek.episodetracker.databinding.DiscoverFragmentBinding
import dev.polek.episodetracker.utils.HideKeyboardScrollListener

class DiscoverFragment : Fragment(), DiscoverView {

    private lateinit var binding: DiscoverFragmentBinding
    private val adapter = DiscoverAdapter()

    private val presenter = App.instance.di.discoverPresenter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View?
    {
        binding = DiscoverFragmentBinding.inflate(inflater)

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(HideKeyboardScrollListener())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        binding.recyclerView.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String): Boolean {
                binding.searchView.clearFocus()
                presenter.onSearchQuerySubmitted(query.trim())
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                return true
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.attachView(this)
    }

    override fun onDestroyView() {
        presenter.detachView()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewAppeared()
    }

    override fun onPause() {
        presenter.onViewDisappeared()
        super.onPause()
    }

    override fun showPrompt() {

    }

    override fun hidePrompt() {

    }

    override fun showProgress() {

    }

    override fun hideProgress() {

    }

    override fun showSearchResults(results: List<DiscoverResultViewModel>) {
        adapter.results = results
    }

    override fun updateSearchResult(result: DiscoverResultViewModel) {

    }

    override fun updateSearchResults() {
        adapter.notifyDataSetChanged()
    }

    override fun showEmptyMessage() {

    }

    override fun hideEmptyMessage() {

    }

    override fun showError() {

    }

    override fun hideError() {

    }

    override fun displayRemoveShowConfirmation(result: DiscoverResultViewModel, callback: (confirmed: Boolean) -> Unit) {

    }

    override fun openDiscoverShow(show: DiscoverResultViewModel) {

    }

    companion object {
        fun instance() = DiscoverFragment()
    }
}
