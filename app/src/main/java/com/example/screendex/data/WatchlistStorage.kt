package com.example.screendex.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class WatchlistStorage(context: Context) {
    private val preferences = context.getSharedPreferences(
        "screendex_watchlist",
        Context.MODE_PRIVATE
    )

    fun load(): List<Movie> {
        val rawJson = preferences.getString(KEY_MOVIES, "[]").orEmpty()
        val jsonArray = JSONArray(rawJson)

        return (0 until jsonArray.length()).map { index ->
            jsonArray.getJSONObject(index).toMovie()
        }
    }

    fun save(movies: List<Movie>) {
        val jsonArray = JSONArray()

        movies.forEach { movie ->
            jsonArray.put(movie.toJson())
        }

        preferences.edit()
            .putString(KEY_MOVIES, jsonArray.toString())
            .apply()
    }

    private fun Movie.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("overview", overview)
            .put("posterUrl", posterUrl)
            .put("backdropUrl", backdropUrl)
            .put("rating", rating)
            .put("releaseYear", releaseYear)
            .put("mediaType", mediaType)
            .put("numberOfSeasons", numberOfSeasons)
    }

    private fun JSONObject.toMovie(): Movie {
        return Movie(
            id = getInt("id"),
            title = optString("title"),
            overview = optString("overview"),
            posterUrl = optString("posterUrl").takeIf { it.isNotBlank() && it != "null" },
            backdropUrl = optString("backdropUrl").takeIf { it.isNotBlank() && it != "null" },
            rating = optString("rating"),
            releaseYear = optString("releaseYear"),
            mediaType = optString("mediaType").ifBlank { "movie" },
            numberOfSeasons = if (isNull("numberOfSeasons")) null else optInt("numberOfSeasons")
        )
    }

    companion object {
        private const val KEY_MOVIES = "movies"
    }
}