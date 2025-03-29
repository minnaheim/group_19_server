package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.config.TMDbConfig;
import ch.uzh.ifi.hase.soprafs24.entity.Movie;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TMDbService {

    private final Logger log = LoggerFactory.getLogger(TMDbService.class);
    private final TMDbConfig tmdbConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public TMDbService(TMDbConfig tmdbConfig, RestTemplate restTemplate) {
        this.tmdbConfig = tmdbConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search for movies in TMDb API
     *
     * @param searchParams Movie object containing search parameters
     * @return List of movies matching the search criteria
     */
    public List<Movie> searchMovies(Movie searchParams) {
        try {
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.error("TMDb API key is missing. Please add it to local.properties");
                return Collections.emptyList();
            }

            // Build URL with search parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(tmdbConfig.getBaseUrl() + "/search/movie")
                    .queryParam("api_key", tmdbConfig.getApiKey());

            // Add search parameters if they exist
            if (searchParams.getTitle() != null && !searchParams.getTitle().isEmpty()) {
                builder.queryParam("query", searchParams.getTitle());
            }
            if (searchParams.getYear() != null) {
                builder.queryParam("year", searchParams.getYear());
            }

            // Set request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Execute the request
            ResponseEntity<String> response = restTemplate.exchange(
                    builder.build().encode().toUri(),
                    HttpMethod.GET,
                    entity,
                    String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode results = root.path("results");
            List<Movie> movies = new ArrayList<>();

            // Process each movie result
            for (JsonNode movieData : results) {
                Movie movie = mapTMDbMovieToEntity(movieData);
                movies.add(movie);
            }

            return movies;
        } catch (RestClientException e) {
            log.error("Error calling TMDb API: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error processing TMDb API response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get detailed movie information from TMDb API
     *
     * @param movieId TMDb movie ID
     * @return Movie entity with detailed information
     */
    public Movie getMovieDetails(int movieId) {
        try {
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.error("TMDb API key is missing. Please add it to local.properties");
                return null;
            }

            // Build URL for movie details
            String url = tmdbConfig.getBaseUrl() + "/movie/" + movieId + "?api_key=" + tmdbConfig.getApiKey() + "&append_to_response=videos,credits";

            // Set request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Execute the request
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class);

            // Parse response and map to Movie entity
            JsonNode movieData = objectMapper.readTree(response.getBody());
            return mapTMDbMovieToEntity(movieData);
        } catch (RestClientException e) {
            log.error("Error fetching movie details from TMDb API: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error processing TMDb movie details: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Maps TMDb API response to Movie entity
     */
    private Movie mapTMDbMovieToEntity(JsonNode movieData) {
        Movie movie = new Movie();

        // Set movie ID from TMDb
        movie.setMovieId(movieData.path("id").asInt());

        // Set title
        movie.setTitle(movieData.path("title").asText(""));

        // Set poster URL
        String posterPath = movieData.path("poster_path").asText(null);
        if (posterPath != null && !posterPath.isEmpty()) {
            movie.setPosterURL("https://image.tmdb.org/t/p/w500" + posterPath);
        }

        // Set description/overview
        movie.setDescription(movieData.path("overview").asText(""));

        // Process release date to extract year
        try {
            String releaseDate = movieData.path("release_date").asText("");
            if (!releaseDate.isEmpty()) {
                LocalDate date = LocalDate.parse(releaseDate, DateTimeFormatter.ISO_DATE);
                movie.setYear(date.getYear());
            }
        } catch (DateTimeParseException e) {
            log.warn("Could not parse release date for movie ID {}", movie.getMovieId());
        }

        // Extract genre information
        JsonNode genresNode = movieData.path("genres");
        if (genresNode.isArray() && genresNode.size() > 0) {
            StringBuilder genres = new StringBuilder();
            for (int i = 0; i < genresNode.size(); i++) {
                if (i > 0) {
                    genres.append(", ");
                }
                genres.append(genresNode.get(i).path("name").asText(""));
            }
            movie.setGenre(genres.toString());
        }

        // Extract trailer URL if available
        JsonNode videos = movieData.path("videos").path("results");
        if (videos.isArray() && videos.size() > 0) {
            for (JsonNode video : videos) {
                if ("YouTube".equals(video.path("site").asText("")) &&
                        "Trailer".equals(video.path("type").asText(""))) {
                    movie.setTrailerURL("https://www.youtube.com/watch?v=" + video.path("key").asText(""));
                    break;
                }
            }
        }

        // Extract actor information if available
        JsonNode credits = movieData.path("credits");
        if (!credits.isMissingNode() && credits.has("cast") && credits.path("cast").isArray()) {
            JsonNode cast = credits.path("cast");
            StringBuilder actors = new StringBuilder();
            int count = 0;
            for (int i = 0; i < cast.size() && count < 5; i++) {
                if (count > 0) {
                    actors.append(", ");
                }
                actors.append(cast.get(i).path("name").asText(""));
                count++;
            }
            movie.setActor(actors.toString());
        }

        // Set language
        if (movieData.has("original_language")) {
            movie.setLanguage(movieData.path("original_language").asText(""));
        }

        // Set production country if available
        JsonNode countries = movieData.path("production_countries");
        if (countries.isArray() && countries.size() > 0) {
            movie.setCountry(countries.get(0).path("name").asText(""));
        }

        return movie;
    }
}