package com.example.screendex

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screendex.data.Movie
import com.example.screendex.data.TmdbRepository
import com.example.screendex.ui.theme.ScreenDexTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.net.URL

private val ScreenDexYellow = Color(0xFFF4C542)
private val ScreenDexInk = Color(0xFF171717)
private val ScreenDexSoftGray = Color(0xFFF2F2F2)

sealed interface Screen {
    data object Home : Screen
    data class Detail(val movie: Movie) : Screen
}

enum class HomeCategory(
    val label: String,
    val searchType: String
) {
    Movies("Film", "movies"),
    Series("Série", "series"),
    Anime("Anime", "anime")
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val selectedCategory: HomeCategory = HomeCategory.Movies,
    val popularMovies: List<Movie> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ScreenDexTheme(dynamicColor = false) {
                ScreenDexApp()
            }
        }
    }
}

@Composable
fun ScreenDexApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        when (val currentScreen = screen) {
            Screen.Home -> {
                Scaffold(containerColor = Color.White) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        onMovieClick = { movie ->
                            screen = Screen.Detail(movie)
                        }
                    )
                }
            }

            is Screen.Detail -> {
                DetailScreen(
                    movie = currentScreen.movie,
                    onBack = {
                        screen = Screen.Home
                    }
                )
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    repository: TmdbRepository = remember { TmdbRepository() },
    onMovieClick: (Movie) -> Unit
) {
    var state by remember { mutableStateOf(HomeUiState()) }

    LaunchedEffect(state.selectedCategory) {
        state = state.copy(
            isLoading = true,
            popularMovies = emptyList(),
            trendingMovies = emptyList(),
            searchQuery = "",
            searchResults = emptyList(),
            errorMessage = null
        )

        state = try {
            val popular = when (state.selectedCategory) {
                HomeCategory.Movies -> repository.getPopularMovies()
                HomeCategory.Series -> repository.getPopularSeries()
                HomeCategory.Anime -> repository.getPopularAnime()
            }

            val trending = when (state.selectedCategory) {
                HomeCategory.Movies -> repository.getTrendingMovies()
                HomeCategory.Series -> repository.getTrendingSeries()
                HomeCategory.Anime -> repository.getTrendingAnime()
            }

            state.copy(
                isLoading = false,
                popularMovies = popular,
                trendingMovies = trending
            )
        } catch (exception: Exception) {
            state.copy(
                isLoading = false,
                errorMessage = exception.message ?: "Impossible de charger les contenus."
            )
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.searchQuery }
            .debounce(500)
            .distinctUntilChanged()
            .collectLatest { query ->
                if (query.length < 2) {
                    state = state.copy(
                        searchResults = emptyList(),
                        isSearching = false
                    )
                    return@collectLatest
                }

                state = state.copy(isSearching = true)

                state = try {
                    state.copy(
                        searchResults = repository.searchMovies(
                            query = query,
                            category = state.selectedCategory.searchType
                        ),
                        isSearching = false,
                        errorMessage = null
                    )
                } catch (exception: Exception) {
                    state.copy(
                        isSearching = false,
                        errorMessage = exception.message ?: "Recherche impossible."
                    )
                }
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Header() }

        item {
            SearchField(
                value = state.searchQuery,
                onValueChange = { query ->
                    state = state.copy(searchQuery = query)
                }
            )
        }

        item {
            CategoryRow(
                selectedCategory = state.selectedCategory,
                onCategorySelected = { category ->
                    state = state.copy(selectedCategory = category)
                }
            )
        }

        if (state.searchQuery.length >= 2) {
            item {
                SectionTitle("Résultats")
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    state.isSearching -> LoadingState()
                    state.searchResults.isEmpty() -> EmptySearchState()
                    else -> {
                        MoviePosterRow(
                            movies = state.searchResults,
                            onMovieClick = onMovieClick
                        )
                    }
                }
            }
        } else {
            when {
                state.isLoading -> {
                    item { LoadingState() }
                }

                state.errorMessage != null -> {
                    item {
                        ErrorState(message = state.errorMessage.orEmpty())
                    }
                }

                else -> {
                    item {
                        SectionTitle("A la une")
                        Spacer(modifier = Modifier.height(12.dp))

                        val featuredMovie = state.popularMovies.firstOrNull()
                        if (featuredMovie != null) {
                            FeaturedMovieCard(
                                movie = featuredMovie,
                                onClick = {
                                    onMovieClick(featuredMovie)
                                }
                            )
                        }
                    }

                    item {
                        SectionTitle("Tendances")
                        Spacer(modifier = Modifier.height(12.dp))
                        MoviePosterRow(
                            movies = state.trendingMovies,
                            onMovieClick = onMovieClick
                        )
                    }

                    item {
                        SectionTitle("Populaires")
                        Spacer(modifier = Modifier.height(12.dp))
                        MoviePosterRow(
                            movies = state.popularMovies.drop(1),
                            onMovieClick = onMovieClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Bonsoir,\nQue regarde-t-on ce soir?",
            modifier = Modifier.weight(1f),
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = ScreenDexInk
        )

        ProfileBubble()
    }
}

@Composable
private fun ProfileBubble() {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(ScreenDexInk),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SD",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text("Rechercher un film")
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun CategoryRow(
    selectedCategory: HomeCategory,
    onCategorySelected: (HomeCategory) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(HomeCategory.entries) { category ->
            CategoryChip(
                text = category.label,
                selected = category == selectedCategory,
                onClick = {
                    onCategorySelected(category)
                }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (selected) ScreenDexYellow else ScreenDexSoftGray)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        color = ScreenDexInk,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = ScreenDexInk
    )
}

@Composable
private fun FeaturedMovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenDexInk)
    ) {
        Box {
            RemoteImage(
                imageUrl = movie.backdropUrl ?: movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                ScreenDexInk.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Note ${movie.rating}",
                    color = ScreenDexYellow,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MoviePosterRow(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        items(
            items = movies.take(12),
            key = { movie -> movie.id }
        ) { movie ->
            MoviePoster(
                movie = movie,
                onClick = {
                    onMovieClick(movie)
                }
            )
        }
    }
}

@Composable
private fun MoviePoster(
    movie: Movie,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = ScreenDexSoftGray)
        ) {
            RemoteImage(
                imageUrl = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = movie.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            color = ScreenDexInk
        )

        if (movie.releaseYear.isNotBlank()) {
            Text(
                text = movie.releaseYear,
                color = ScreenDexInk.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DetailScreen(
    movie: Movie,
    onBack: () -> Unit,
    repository: TmdbRepository = remember { TmdbRepository() }
) {
    var detailedMovie by remember(movie.id) { mutableStateOf(movie) }
    var isLoadingDetails by remember(movie.id) { mutableStateOf(true) }

    LaunchedEffect(movie.id, movie.mediaType) {
        isLoadingDetails = true
        detailedMovie = try {
            repository.getDetails(movie)
        } catch (exception: Exception) {
            movie
        }
        isLoadingDetails = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
                    .background(ScreenDexInk)
            ) {
                RemoteImage(
                    imageUrl = detailedMovie.backdropUrl ?: detailedMovie.posterUrl,
                    contentDescription = detailedMovie.title,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ScreenDexInk.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Text(
                    text = "Retour",
                    modifier = Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = ScreenDexInk,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(22.dp)
                ) {
                    Text(
                        text = detailedMovie.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Note ${detailedMovie.rating}",
                        color = ScreenDexYellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip("Note ${detailedMovie.rating}")

                    if (detailedMovie.releaseYear.isNotBlank()) {
                        InfoChip(detailedMovie.releaseYear)
                    }

                    detailedMovie.numberOfSeasons?.let { seasons ->
                        InfoChip("$seasons saison${if (seasons > 1) "s" else ""}")
                    }
                }

                if (isLoadingDetails) {
                    Text(
                        text = "Chargement des détails...",
                        color = ScreenDexInk.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = "Synopsis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScreenDexInk
                )

                Text(
                    text = detailedMovie.overview,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    color = ScreenDexInk
                )
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ScreenDexSoftGray)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = ScreenDexInk,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RemoteImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUrl) {
        bitmap = if (imageUrl.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(imageUrl).openStream().use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }.getOrNull()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(
                    colors = listOf(ScreenDexYellow, ScreenDexInk)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ScreenDex",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ScreenDexInk)
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF1F1))
            .padding(18.dp)
    ) {
        Text(
            text = "Erreur",
            color = Color(0xFF8A1C1C),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            color = Color(0xFF8A1C1C)
        )
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Aucun résultat",
            color = ScreenDexInk.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenDexPreview() {
    ScreenDexTheme(dynamicColor = false) {
        ScreenDexApp()
    }
}