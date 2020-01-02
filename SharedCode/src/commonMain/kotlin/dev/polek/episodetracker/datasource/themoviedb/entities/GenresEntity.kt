package dev.polek.episodetracker.datasource.themoviedb.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GenresEntity(
    @SerialName("genres") val genres: List<GenreEntity>
)
