package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.config.TMDbConfig;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
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
            // Don't search if no API key is configured
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.warn("TMDB API key is not configured. Skipping external search.");
                return Collections.emptyList();
            }

            if (searchParams.getTitle() != null) {
                String searchEndpoint = tmdbConfig.getBaseUrl() + "/search/movie";
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(searchEndpoint)
                        .queryParam("sort_by", "popularity.desc");

                builder.queryParam("query", searchParams.getTitle().trim());

                // Setup authentication headers
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(tmdbConfig.getApiKey());
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Make the API call
                ResponseEntity<String> response = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        entity,
                        String.class);

                // Parse the response
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode results = root.path("results");

                    List<Movie> movies = new ArrayList<>();
                    for (JsonNode movieNode : results) {
                        Movie movie = mapTMDbMovieToEntity(movieNode);

                        // Apply genre filter if specified
                        if (searchParams.getGenre() != null && !searchParams.getGenre().isEmpty() && movie.getGenre() != null) {
                            if (!movie.getGenre().toLowerCase().contains(searchParams.getGenre().toLowerCase())) {
                                continue;
                            }
                        }

                        movies.add(movie);
                    }

                    return movies;
                }
            }
            else if (searchParams.getTitle() == null) {
                String searchEndpoint = tmdbConfig.getBaseUrl() + "/discover/movie";
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(searchEndpoint)
                        .queryParam("sort_by", "popularity.desc");

                // Add search parameters if available
                // years
                if (searchParams.getYear() != null) {
                    builder.queryParam("year", searchParams.getYear());
                }

                // genre
                if (searchParams.getGenre() != null && !searchParams.getGenre().isEmpty()) {
                    builder.queryParam("with_genres", searchParams.getGenre());
                }

                // actor
                if (searchParams.getActor() != null && !searchParams.getActor().isEmpty()) {
                    builder.queryParam("with_cast", searchParams.getActor());
                }

                // director
                if (searchParams.getDirector() != null && !searchParams.getDirector().isEmpty()) {
                    builder.queryParam("with_crew", searchParams.getDirector());
                }


                // Setup authentication headers
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(tmdbConfig.getApiKey());
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Make the API call
                ResponseEntity<String> response = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        entity,
                        String.class);

                // Parse the response
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode results = root.path("results");

                    List<Movie> movies = new ArrayList<>();
                    for (JsonNode movieNode : results) {
                        Movie movie = mapTMDbMovieToEntity(movieNode);

                        // Apply genre filter if specified
                        if (searchParams.getGenre() != null && !searchParams.getGenre().isEmpty() && movie.getGenre() != null) {
                            if (!movie.getGenre().toLowerCase().contains(searchParams.getGenre().toLowerCase())) {
                                continue;
                            }
                        }

                        movies.add(movie);
                    }

                    return movies;
                }
            }
            return Collections.emptyList();
        }
        catch (RestClientException e) {
            log.error("Error communicating with TMDb API: {}", e.getMessage());
            return Collections.emptyList();
        }
        catch (Exception e) {
            log.error("Unexpected error during TMDb search: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get detailed movie information from TMDb API
     *
     * @param movieId TMDb movie ID
     * @return Movie entity with detailed information
     */
    public Movie getMovieDetails(long movieId) {
        try {
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.warn("TMDB API key is not configured. Cannot get movie details.");
                return null;
            }

            String detailsEndpoint = tmdbConfig.getBaseUrl() + "/movie/" + movieId;

            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(detailsEndpoint)
                    .queryParam("append_to_response", "videos,credits");


            // Setup authentication headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tmdbConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    detailsEndpoint,
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode movieData = objectMapper.readTree(response.getBody());
                return mapTMDbMovieToEntity(movieData);
            }

            return null;
        }
        catch (Exception e) {
            log.error("Error getting movie details from TMDb: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get list of all genres from TMDb
     */
    public JsonNode getGenres() {
        try {
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.warn("TMDB API key is not configured. Cannot get genres.");
                return null;
            }

            String genresEndpoint = tmdbConfig.getBaseUrl() + "/genre/movie/list";

            // Setup authentication headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tmdbConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    genresEndpoint,
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("genres");
            }

            return null;
        }
        catch (Exception e) {
            log.error("Error getting genres from TMDb: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Maps TMDb API response to Movie entity
     */
    private Movie mapTMDbMovieToEntity(JsonNode movieData) {
        Movie movie = new Movie();

        movie.setMovieId(movieData.path("id").asLong());
        movie.setTitle(movieData.path("title").asText());
        movie.setDescription(movieData.path("overview").asText());

        // Extract release year from release_date (YYYY-MM-DD)
        String releaseDate = movieData.path("release_date").asText();
        if (releaseDate != null && !releaseDate.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(releaseDate, DateTimeFormatter.ISO_LOCAL_DATE);
                movie.setYear(date.getYear());
            } catch (DateTimeParseException e) {
                log.warn("Could not parse release date: {}", releaseDate);
            }
        }

        // Extract genre names from genre_ids
        if (movieData.has("genre_ids") && movieData.path("genre_ids").isArray()) {
            StringBuilder genreNames = new StringBuilder();
            JsonNode genreIds = movieData.path("genre_ids");

            for (JsonNode genreId : genreIds) {
                // Here we would ideally map genre IDs to names, but for simplicity
                // we'll just store the IDs as a comma-separated string
                if (genreNames.length() > 0) {
                    genreNames.append(", ");
                }
                genreNames.append(genreId.asText());
            }
            movie.setGenre(genreNames.toString());
        }

        // Set poster URL if available
        String posterPath = movieData.path("poster_path").asText(null);
        if (posterPath != null && !posterPath.isEmpty()) {
            movie.setPosterURL("https://image.tmdb.org/t/p/w500" + posterPath);
        }

        // Original language as the language
        movie.setOriginallanguage(movieData.path("original_language").asText());

        return movie;
    }
}