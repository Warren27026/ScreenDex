package com.example.screendex.data

import com.example.screendex.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
/* je ne dois pas oublié que ce fichier contient :
appels API TMDB
header Authorization: Bearer
films populaires
tendances
recherche
détail film*/
class TmdbRepository {
    suspend fun getPopularMovies(): List<Movie> {
        return getMovieList("movie/popular?language=fr-FR&page=1&region=FR")
    }

    suspend fun getTrendingMovies(): List<Movie> {
        return getMovieList("trending/movie/week?language=fr-FR")
    }

    suspend fun searchMovies(query: String): List<Movie> {
        if (query.isBlank()) return emptyList()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        return getMovieList("search/movie?query=$encodedQuery&language=fr-FR&page=1&include_adult=false&region=FR")
    }

    suspend fun getMovieDetails(movieId: Int): Movie {
        val json = getJson("movie/$movieId?language=fr-FR")
        return json.toMovieDto().toMovie()
    }

    private suspend fun getMovieList(pathAndQuery: String): List<Movie> {
        val json = getJson(pathAndQuery)
        val results = json.getJSONArray("results")
        return (0 until results.length()).map { index ->
            results.getJSONObject(index).toMovieDto().toMovie()
        }
    }

    private suspend fun getJson(pathAndQuery: String): JSONObject = withContext(Dispatchers.IO) {
        val token = BuildConfig.TMDB_READ_ACCESS_TOKEN
        require(token.isNotBlank()) {
            "TMDB_READ_ACCESS_TOKEN est manquant. Ajoute-le dans local.properties."
        }

        val connection = URL("$BASE_URL$pathAndQuery").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 12_000
        connection.readTimeout = 12_000

        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            if (responseCode !in 200..299) {
                error("Erreur TMDB $responseCode: $body")
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"
    }
}

private fun JSONObject.toMovieDto(): TmdbMovieDto {
    return TmdbMovieDto(
        id = getInt("id"),
        title = optString("title").ifBlank { optString("name") },
        overview = optString("overview"),
        posterPath = optString("poster_path").takeIf { it.isNotBlank() && it != "null" },
        backdropPath = optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" },
        voteAverage = optDouble("vote_average", Double.NaN).takeIf { !it.isNaN() },
        releaseDate = optString("release_date").ifBlank { optString("first_air_date") },
    )
}
