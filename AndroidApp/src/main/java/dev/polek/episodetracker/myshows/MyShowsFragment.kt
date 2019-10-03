package dev.polek.episodetracker.myshows

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import dev.polek.episodetracker.databinding.MyShowsFragmentBinding
import dev.polek.episodetracker.utils.HideKeyboardScrollListener
import dev.polek.episodetracker.utils.dp2px

class MyShowsFragment : Fragment() {

    private val showsAdapter = MyShowsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View?
    {
        val binding: MyShowsFragmentBinding = MyShowsFragmentBinding.inflate(inflater)

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = showsAdapter
            addOnScrollListener(HideKeyboardScrollListener())
        }

        binding.appBarLayout.outlineProvider = null

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String): Boolean {
                setSearchViewBehaviour(binding, showAlways = query.isNotBlank())
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.searchView.clearFocus()
                return true
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val layoutParams = binding.searchBar.layoutParams as AppBarLayout.LayoutParams
            val searchBarVerticalPadding = requireContext().dp2px(8f)
            layoutParams.topMargin = searchBarVerticalPadding + insets.systemWindowInsetTop
            binding.searchBar.layoutParams = layoutParams

            binding.recyclerView.setPadding(0, 2 * searchBarVerticalPadding + binding.searchBar.height + insets.systemWindowInsetTop, 0, 0)
            binding.recyclerView.scrollToPosition(0)

            return@setOnApplyWindowInsetsListener insets
        }

        return binding.root
    }

    private fun setSearchViewBehaviour(binding: MyShowsFragmentBinding, showAlways: Boolean) {
        val layoutParams = binding.searchBar.layoutParams as AppBarLayout.LayoutParams
        layoutParams.scrollFlags = if (showAlways) 0 else SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
    }

    companion object {
        fun instance() = MyShowsFragment()
    }
}
