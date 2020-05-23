package dev.polek.episodetracker.towatch

import android.text.Html
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.NoTransition
import dev.polek.episodetracker.R
import dev.polek.episodetracker.common.presentation.towatch.ToWatchShowViewModel
import dev.polek.episodetracker.databinding.ToWatchShowLayoutBinding
import dev.polek.episodetracker.utils.doOnClick
import dev.polek.episodetracker.utils.layoutInflater

class ToWatchAdapter : RecyclerView.Adapter<ToWatchAdapter.ViewHolder>() {

    var listener: Listener? = null

    var shows: List<ToWatchShowViewModel> = emptyList()
        set(value) {
            val diff = DiffUtil.calculateDiff(DiffUtilCallback(oldList = shows, newList = value))
            field = value
            diff.dispatchUpdatesTo(this)
        }

    override fun getItemCount() = shows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ToWatchShowLayoutBinding.inflate(parent.layoutInflater, parent, false)
        return ViewHolder(
            binding,
            onShowClicked = { position ->
                listener?.onShowClicked(shows[position])
            },
            onCheckButtonClicked = { position ->
                listener?.onCheckButtonClicked(shows[position])
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val show = shows[position]
        val binding = holder.binding

        binding.title.text = show.name

        @Suppress("DEPRECATION")
        binding.subtitle.text = Html.fromHtml("<b>${show.nextEpisodeNumberText}</b> | ${show.nextEpisodeName}")

        binding.episodesCount.text = show.episodeCount.toString()
        binding.episodesLabel.text = holder.itemView.resources.getQuantityString(R.plurals.episodes, show.episodeCount)

        binding.specialsBadge.isVisible = show.isSpecials


        Glide.with(holder.itemView)
            .load(show.imageUrl)
            .transition(DrawableTransitionOptions.with { dataSource, isFirstResource ->
                when (dataSource) {
                    DataSource.REMOTE -> DrawableCrossFadeFactory.Builder(200).build().build(dataSource, isFirstResource)
                    else -> NoTransition.get()
                }
            })
            .into(binding.image)
    }

    class ViewHolder(
        val binding: ToWatchShowLayoutBinding,
        onShowClicked: (position: Int) -> Unit,
        onCheckButtonClicked: (position: Int) -> Unit) : RecyclerView.ViewHolder(binding.root)
    {
        init {
            binding.root.doOnClick {
                val position = adapterPosition
                if (position == RecyclerView.NO_POSITION) return@doOnClick
                onShowClicked(position)
            }

            binding.checkButton.doOnClick {
                val position = adapterPosition
                if (position == RecyclerView.NO_POSITION) return@doOnClick
                onCheckButtonClicked(position)
            }
        }
    }

    interface Listener {
        fun onShowClicked(show: ToWatchShowViewModel)
        fun onCheckButtonClicked(show: ToWatchShowViewModel)
    }

    private class DiffUtilCallback(
        val oldList: List<ToWatchShowViewModel>,
        val newList: List<ToWatchShowViewModel>) : DiffUtil.Callback()
    {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}