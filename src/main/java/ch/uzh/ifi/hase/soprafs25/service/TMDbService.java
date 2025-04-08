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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TMDbService {

    private final Logger log = LoggerFactory.getLogger(TMDbService.class);
    private final TMDbConfig tmdbConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Map to convert genre IDs to strings
    private static final Map<Integer, String> GENRE_ID_TO_NAME = new HashMap<>();
    static {
        GENRE_ID_TO_NAME.put(28, "Action");
        GENRE_ID_TO_NAME.put(12, "Adventure");
        GENRE_ID_TO_NAME.put(16, "Animation");
        GENRE_ID_TO_NAME.put(35, "Comedy");
        GENRE_ID_TO_NAME.put(80, "Crime");
        GENRE_ID_TO_NAME.put(99, "Documentary");
        GENRE_ID_TO_NAME.put(18, "Drama");
        GENRE_ID_TO_NAME.put(10751, "Family");
        GENRE_ID_TO_NAME.put(14, "Fantasy");
        GENRE_ID_TO_NAME.put(36, "History");
        GENRE_ID_TO_NAME.put(27, "Horror");
        GENRE_ID_TO_NAME.put(10402, "Music");
        GENRE_ID_TO_NAME.put(9648, "Mystery");
        GENRE_ID_TO_NAME.put(10749, "Romance");
        GENRE_ID_TO_NAME.put(878, "Science Fiction");
        GENRE_ID_TO_NAME.put(10770, "TV Movie");
        GENRE_ID_TO_NAME.put(53, "Thriller");
        GENRE_ID_TO_NAME.put(10752, "War");
        GENRE_ID_TO_NAME.put(37, "Western");
    }

    // Map to convert genre names to IDs
    private static final Map<String, String> GENRE_NAME_TO_ID = new HashMap<>();
    static {
        GENRE_NAME_TO_ID.put("Action", "28");
        GENRE_NAME_TO_ID.put("Adventure", "12");
        GENRE_NAME_TO_ID.put("Animation", "16");
        GENRE_NAME_TO_ID.put("Comedy", "35");
        GENRE_NAME_TO_ID.put("Crime", "80");
        GENRE_NAME_TO_ID.put("Documentary", "99");
        GENRE_NAME_TO_ID.put("Drama", "18");
        GENRE_NAME_TO_ID.put("Family", "10751");
        GENRE_NAME_TO_ID.put("Fantasy", "14");
        GENRE_NAME_TO_ID.put("History", "36");
        GENRE_NAME_TO_ID.put("Horror", "27");
        GENRE_NAME_TO_ID.put("Music", "10402");
        GENRE_NAME_TO_ID.put("Mystery", "9648");
        GENRE_NAME_TO_ID.put("Romance", "10749");
        GENRE_NAME_TO_ID.put("Science Fiction", "878");
        GENRE_NAME_TO_ID.put("TV Movie", "10770");
        GENRE_NAME_TO_ID.put("Thriller", "53");
        GENRE_NAME_TO_ID.put("War", "10752");
        GENRE_NAME_TO_ID.put("Western", "37");
    }

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
                    builder.queryParam("primary_release_year", searchParams.getYear());
                }

                // genre
                if (searchParams.getGenres() != null && !searchParams.getGenres().isEmpty()) {
                    // Convert genre names to IDs and join with comma
                    String genreIds = searchParams.getGenres().stream()
                            .map(genre -> GENRE_NAME_TO_ID.getOrDefault(genre, ""))
                            .filter(id -> !id.isEmpty())
                            .collect(Collectors.joining(","));

                    if (!genreIds.isEmpty()) {
                        builder.queryParam("with_genres", genreIds);
                    }
                }


                // actor
                if (searchParams.getActors() != null && !searchParams.getActors().isEmpty()) {
                    builder.queryParam("with_cast", searchParams.getActors());
                }
                // TODO: implement actor search with person IDs
                // This would require additional API calls to convert actor names to IDs


                // director
                if (searchParams.getDirectors() != null && !searchParams.getDirectors().isEmpty()) {
                    builder.queryParam("with_crew", searchParams.getDirectors());
                }
                // TODO: implement director search with person IDs
                // This would require additional API calls to convert director names to IDs


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

                        // ToDo, analyse whether filters need to be applied here
                        /*if (searchParams.getGenres() != null && !searchParams.getGenres().isEmpty() && movie.getGenres() != null) {
                            if (!movie.getGenres().contains(searchParams.getGenres())) {
                                continue;
                            }
                        }
                        */

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
        try {
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

            // Convert genre IDs to genre names
            JsonNode genreIdsNode = movieData.path("genre_ids");
            if (genreIdsNode.isArray()) {
                for (JsonNode genreIdNode : genreIdsNode) {
                    int genreId = genreIdNode.asInt();
                    String genreName = GENRE_ID_TO_NAME.getOrDefault(genreId, "Unknown");
                    movie.addGenre(genreName);
                }
            } else {
                // For other endpoints not yet used but kept for future usages (not delete as else condition should never be true anyways)
                // ToDo delete if not needed
                JsonNode genresNode = movieData.path("genres");
                if (genresNode.isArray()) {
                    for (JsonNode genreNode : genresNode) {
                        int genreId = genreNode.path("id").asInt();
                        String genreName = genreNode.path("name").asText();
                        // If name is provided directly, use it; otherwise look up in our map
                        if (genreName == null || genreName.isEmpty()) {
                            genreName = GENRE_ID_TO_NAME.getOrDefault(genreId, "Unknown");
                        }
                        movie.addGenre(genreName);
                    }
                }
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
        catch (Exception e) {
            log.error("Error mapping TMDb movie to entity: {}", e.getMessage());
            return null;
        }
    }
}