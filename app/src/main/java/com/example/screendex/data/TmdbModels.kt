package com.example.screendex.data

data class TmdbMovieDto(
    val id: Int,
    val title: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double?,
    val releaseDate: String?,
    val numberOfSeasons: Int?
)

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: String,
    val releaseYear: String,
    val mediaType: String,
    val numberOfSeasons: Int? = null
)

fun TmdbMovieDto.toMovie(mediaType: String = "movie"): Movie {
    return Movie(
        id = id,
        title = title.orEmpty().ifBlank { "Titre inconnu" },
        overview = overview.orEmpty().ifBlank { "Aucun synopsis disponible." },
        posterUrl = posterPath?.let { TmdbImage.poster(it) },
        backdropUrl = backdropPath?.let { TmdbImage.backdrop(it) },
        rating = voteAverage?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "-",
        releaseYear = releaseDate?.take(4).orEmpty(),
        mediaType = mediaType,
        numberOfSeasons = numberOfSeasons
    )
}

object TmdbImage {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"

    fun poster(path: String): String = "${BASE_URL}w500$path"

    fun backdrop(path: String): String = "${BASE_URL}w780$path"
}