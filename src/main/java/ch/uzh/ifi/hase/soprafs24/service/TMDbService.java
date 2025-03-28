package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.config.TMDbConfig;
import ch.uzh.ifi.hase.soprafs24.entity.Movie;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TMDbService {

    private final Logger log = LoggerFactory.getLogger(TMDbService.class);
    private final TMDbConfig tmdbConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public TMDbService(TMDbConfig tmdbConfig) {
        this.tmdbConfig = tmdbConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search for movies in TMDb API
     *
     * @param title Movie title to search for
     * @param year Year of release (optional)
     * @return List of movies matching the search criteria
     */
    public List<Movie> searchMovies(String title, Integer year) {
        List<Movie> movies = new ArrayList<>();

        try {
            String url = String.format("%s/search/movie?api_key=%s&query=%s",
                    tmdbConfig.getBaseUrl(), tmdbConfig.getApiKey(), title);

            if (year != null) {
                url += "&year=" + year;
            }

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");

            if (results != null && results.isArray()) {
                for (JsonNode result : results) {
                    Movie movie = mapTMDbMovieToEntity(result);
                    movies.add(movie);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching movies from TMDb: {}", e.getMessage());
        }

        return movies;
    }

    /**
     * Get detailed movie information from TMDb API
     *
     * @param movieId TMDb movie ID
     * @return Movie entity with detailed information
     */
    public Movie getMovieDetails(int movieId) {
        try {
            String url = String.format("%s/movie/%d?api_key=%s&append_to_response=videos,credits",
                    tmdbConfig.getBaseUrl(), movieId, tmdbConfig.getApiKey());

            String response = restTemplate.getForObject(url, String.class);
            JsonNode movieData = objectMapper.readTree(response);

            return mapTMDbMovieToEntity(movieData);
        } catch (Exception e) {
            log.error("Error fetching movie details from TMDb: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Maps TMDb API response to Movie entity
     */
    private Movie mapTMDbMovieToEntity(JsonNode movieData) {
        Movie movie = new Movie();

        // Map basic movie info
        movie.setMovieId(movieData.get("id").asInt());
        movie.setTitle(movieData.get("title").asText());
        movie.setDescription(movieData.get("overview").asText());

        // Map poster URL
        if (movieData.has("poster_path") && !movieData.get("poster_path").isNull()) {
            movie.setPosterURL("https://image.tmdb.org/t/p/w500" + movieData.get("poster_path").asText());
        }

        // Map release year
        if (movieData.has("release_date") && !movieData.get("release_date").isNull()
                && !movieData.get("release_date").asText().isEmpty()) {
            String releaseDate = movieData.get("release_date").asText();
            movie.setYear(Integer.parseInt(releaseDate.split("-")[0]));
        }

        // Map genre
        if (movieData.has("genres") && movieData.get("genres").isArray() && movieData.get("genres").size() > 0) {
            StringBuilder genreBuilder = new StringBuilder();
            for (JsonNode genre : movieData.get("genres")) {
                if (genreBuilder.length() > 0) genreBuilder.append(", ");
                genreBuilder.append(genre.get("name").asText());
            }
            movie.setGenre(genreBuilder.toString());
        } else if (movieData.has("genre_ids") && movieData.get("genre_ids").isArray() && movieData.get("genre_ids").size() > 0) {
            // For search results, we just store the IDs as temporary placeholders
            movie.setGenre(movieData.get("genre_ids").toString());
        }

        // Map trailer URL if available (from videos data)
        if (movieData.has("videos") && movieData.get("videos").has("results")) {
            JsonNode videos = movieData.get("videos").get("results");
            for (JsonNode video : videos) {
                if ("YouTube".equals(video.get("site").asText()) && "Trailer".equals(video.get("type").asText())) {
                    movie.setTrailerURL("https://www.youtube.com/watch?v=" + video.get("key").asText());
                    break;
                }
            }
        }

        // Map actors if available (from credits data)
        if (movieData.has("credits") && movieData.get("credits").has("cast")) {
            JsonNode cast = movieData.get("credits").get("cast");
            StringBuilder actorBuilder = new StringBuilder();
            int count = 0;
            for (JsonNode actor : cast) {
                if (count++ >= 5) break;  // Limit to top 5 actors
                if (actorBuilder.length() > 0) actorBuilder.append(", ");
                actorBuilder.append(actor.get("name").asText());
            }
            movie.setActor(actorBuilder.toString());
        }

        // Other fields might need additional API calls or might not be available
        // Language
        if (movieData.has("original_language")) {
            movie.setLanguage(movieData.get("original_language").asText());
        }

        // Country of origin might require additional API call, so we leave it null for now

        return movie;
    }
}