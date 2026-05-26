package com.example.screendex

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.compose.foundation.clickable

private val ScreenDexYellow = Color(0xFFF4C542)
private val ScreenDexInk = Color(0xFF171717)
private val ScreenDexSoftGray = Color(0xFFF2F2F2)

data class HomeUiState(
    val isLoading: Boolean = true,
    val selectedCategory: HomeCategory = HomeCategory.Movies,
    val popularMovies: List<Movie> = emptyList(),
    val trendingMovies: List<Movie> = emptyList(),
    val errorMessage: String? = null
)
enum class HomeCategory(
    val label: String
) {
    Movies("Film"),
    Series("Série"),
    Anime("Anime")
}

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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Scaffold(
            containerColor = Color.White
        ) { innerPadding ->
            HomeScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    repository: TmdbRepository = remember { TmdbRepository() }
) {
    var state by remember { mutableStateOf(HomeUiState()) }

    LaunchedEffect(state.selectedCategory) {
        state = state.copy(
            isLoading = true,
            popularMovies = emptyList(),
            trendingMovies = emptyList(),
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Header()
        }

        item {
            SearchPlaceholder()
        }

        item {
            CategoryRow(
                selectedCategory = state.selectedCategory,
                onCategorySelected = { category ->
                    state = state.copy(selectedCategory = category)
                }
            )
        }

        when {
            state.isLoading -> {
                item {
                    LoadingState()
                }
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
                        FeaturedMovieCard(movie = featuredMovie)
                    }
                }

                item {
                    SectionTitle("Tendances")
                    Spacer(modifier = Modifier.height(12.dp))
                    MoviePosterRow(movies = state.trendingMovies)
                }

                item {
                    SectionTitle("Populaires")
                    Spacer(modifier = Modifier.height(12.dp))
                    MoviePosterRow(movies = state.popularMovies.drop(1))
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun SearchPlaceholder() {
    Text(
        text = "Rechercher un film",
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ScreenDexSoftGray)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        color = ScreenDexInk.copy(alpha = 0.6f)
    )
}

@Composable
private fun CategoryRow(
    selectedCategory: HomeCategory,
    onCategorySelected: (HomeCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
private fun FeaturedMovieCard(movie: Movie) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ScreenDexInk
        )
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
private fun MoviePosterRow(movies: List<Movie>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(
            items = movies.take(12),
            key = { movie -> movie.id }
        ) { movie ->
            MoviePoster(movie = movie)
        }
    }
}

@Composable
private fun MoviePoster(movie: Movie) {
    Column(
        modifier = Modifier.width(118.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(166.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = ScreenDexSoftGray
            )
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
        CircularProgressIndicator(
            color = ScreenDexInk
        )
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

@Preview(showBackground = true)
@Composable
fun ScreenDexPreview() {
    ScreenDexTheme(dynamicColor = false) {
        ScreenDexApp()
    }
}