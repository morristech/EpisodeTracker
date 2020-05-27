package dev.polek.episodetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.polek.episodetracker.common.presentation.app.AppPresenter
import dev.polek.episodetracker.common.presentation.main.MainPresenter
import dev.polek.episodetracker.common.presentation.main.MainView
import dev.polek.episodetracker.databinding.MainActivityBinding
import dev.polek.episodetracker.discover.DiscoverFragment
import dev.polek.episodetracker.myshows.MyShowsFragment
import dev.polek.episodetracker.settings.SettingsFragment
import dev.polek.episodetracker.towatch.ToWatchFragment

class MainActivity : AppCompatActivity(), MainView {

    private val presenter: MainPresenter = App.instance.di.mainPresenter()
    private val appPresenter: AppPresenter = App.instance.di.appPresenter()
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = PageAdapter(this)
        binding.pager.offscreenPageLimit = NUMBER_OF_PAGES - 1
        binding.pager.isUserInputEnabled = false

        binding.bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            val pagePosition = when (menuItem.itemId) {
                R.id.action_my_shows -> 0
                R.id.action_to_watch -> 1
                R.id.action_discover -> 2
                R.id.action_settings -> 3
                else -> throw NotImplementedError("Unknown menu item: $menuItem")
            }
            binding.pager.setCurrentItem(pagePosition, false)
            return@setOnNavigationItemSelectedListener true
        }

        presenter.attachView(this)
    }

    override fun onDestroy() {
        presenter.detachView()
        binding.bottomNavigation.setOnNavigationItemSelectedListener(null)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewAppeared()
    }

    override fun onPause() {
        presenter.onViewDisappeared()
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        appPresenter.onViewAppeared()
    }

    override fun onStop() {
        appPresenter.onViewDisappeared()
        super.onStop()
    }

    ///////////////////////////////////////////////////////////////////////////
    //region MainView implementation

    override fun showToWatchBadge(count: Int) {
        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.action_to_watch)
        badge.number = count
    }

    override fun hideToWatchBadge() {
        binding.bottomNavigation.removeBadge(R.id.action_to_watch)
    }
    //endregion
    ///////////////////////////////////////////////////////////////////////////

    private class PageAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount() = NUMBER_OF_PAGES

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> MyShowsFragment.instance()
            1 -> ToWatchFragment.instance()
            2 -> DiscoverFragment.instance()
            3 -> SettingsFragment.instance()
            else -> throw NotImplementedError("Page #$position not implemented")
        }
    }

    companion object {
        private const val NUMBER_OF_PAGES = 4
    }
}
