package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.exceptions.SearchValidationException;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import ch.uzh.ifi.hase.soprafs25.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import java.util.ArrayList;

/**
 * Movie Service
 * This class is the "worker" and responsible for all functionality related to the movie
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class MovieService {

    private final Logger log = LoggerFactory.getLogger(MovieService.class);
    private final MovieRepository movieRepository;
    private final TMDbService tmdbService;
    private final UserRepository userRepository;

    @Autowired
    public MovieService(@Qualifier("movieRepository") MovieRepository movieRepository, TMDbService tmdbService, UserRepository userRepository) {
        this.movieRepository = movieRepository;
        this.tmdbService = tmdbService;
        this.userRepository = userRepository;
    }

    /**
     * Get movies based on search criteria from only TMDb API
     *
     * @param searchParams Movie object containing search parameters searchparams are of type title: string, genres: List<string> year: integer, actors: List<long>, directors: List<long>
     *                     searchparams cannot be spokenlanguages: List<string>!
     * @return List of movies matching the search criteria without duplicates
     */
    public List<Movie> getMovies(Movie searchParams) {
        // Search in TMDb API
        return tmdbService.searchMovies(searchParams);
    }

    /**
     * Get a specific movie by its ID
     * First tries to get from local DB, then falls back to TMDb
     *
     * @param movieId The movie's unique identifier (TMDb ID)
     * @return The movie if found
     * @throws ResponseStatusException if the movie does not exist
     */
    public Movie getMovieById(long movieId) {
        // Try to get from local DB first
        Movie movie = movieRepository.findByMovieId(movieId);

        // If not found locally, try TMDb
        if (movie == null) {
            try {
                movie = tmdbService.getMovieDetails(movieId);

            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Movie with ID " + movieId + " was not found");
            }
        }

        if (movie == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Movie with ID " + movieId + " was not found");
        }

        return movie;
    }


    /**
     * Saves a movie to the local database
     * Use this method for movies that need to be persisted (e.g., favorites)
     *
     * @param movie The movie to save
     * @return The saved movie
     */
    public Movie saveMovie(Movie movie) {
        // Set required fields if not set
        if (movie.getMovieId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie ID is required");
        }

        boolean isTrailerBlank = (movie.getTrailerURL() == null || movie.getTrailerURL().isBlank());
        boolean isActorsEmpty = (movie.getActors() == null ||movie.getActors().stream().allMatch(item -> item == null || item.trim().isEmpty()));
        boolean isDirectorsEmpty = (movie.getDirectors() == null || movie.getDirectors().stream().allMatch(item -> item == null || item.trim().isEmpty()));

        if (isTrailerBlank  && isActorsEmpty && isDirectorsEmpty) {
            throw new SearchValidationException("Movie must be saved with details available from TMDb API. Missing trailer, actors or directors.");
        }

        // Check if movie already exists
        Movie existingMovie = movieRepository.findByMovieId(movie.getMovieId());
        if (existingMovie != null) {
            return existingMovie; // Return existing movie to avoid duplicates
        }

        // Save the movie
        return movieRepository.save(movie);
    }

    /**
     * Get personalized movie suggestions for a user
     * New method to generate movie suggestions based on user favorites
     *
     * @param userId User ID for which to generate suggestions
     * @param limit Maximum number of suggestions to return
     * @return List of suggested movies
     */
    public List<Movie> getMovieSuggestions(Long userId, int limit) {
        // Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User with ID " + userId + " not found"));

        log.info("Found user {}", user);

        // Create sets of movies to exclude (already watched or in watchlist)
        Set<Long> excludedMovieIds = new HashSet<>();
        if (user.getWatchedMovies() != null) {
            user.getWatchedMovies().forEach(movie -> excludedMovieIds.add(movie.getMovieId()));
        }
        if (user.getWatchlist() != null) {
            user.getWatchlist().forEach(movie -> excludedMovieIds.add(movie.getMovieId()));
        }

        // Collect all user favorites
        List<String> favoriteGenres = user.getFavoriteGenres();
        log.info("getMovieSuggestions: Favorite genres are {}", favoriteGenres);
        List<String> favoriteActorNames = user.getFavoriteActors();
        log.info("getMovieSuggestions: Favorite actornames are {}", favoriteActorNames);
        List<String> favoriteDirectorNames = user.getFavoriteDirectors();
        log.info("getMovieSuggestions: Favorite directornames are {}", favoriteDirectorNames);

        List<String> favoriteActorIds = HelperMethodFindActorIds(favoriteActorNames);
        log.info("getMovieSuggestions: Favorite actorid's are {}", favoriteActorIds);
        List<String> favoriteDirectorIds = HelperMethodFindDirectorIds(favoriteDirectorNames);
        log.info("getMovieSuggestions: Favorite directorid's are {}", favoriteDirectorIds);

        // Maximum number of API calls to prevent excessive requests
        final int MAX_API_CALLS = 200;
        int apiCallCount = 0;

        // Resulting list of suggested movies
        Set<Movie> suggestions = new HashSet<>();

        // Generate all permutations of search parameters, starting with most specific
        List<Movie> searchQueries = generateSearchPermutations(favoriteGenres, favoriteActorIds, favoriteDirectorIds);

        log.info("Generated {} search permutations for user {}: {}", searchQueries.size(), userId, searchQueries);

        int remainingQueries = searchQueries.size();
        int remainingLimit = limit;

        // Execute searches in order until we have enough suggestions or reach API call limit
        for (Movie searchParams : searchQueries) {
            log.info("Processing search query {} of {}", (searchQueries.size() - remainingQueries + 1), searchQueries.size());

            if (suggestions.size() >= limit || apiCallCount >= MAX_API_CALLS) {
                break;
            }

            // Calculate quota for current query - distribute remaining limit evenly among remaining queries
            // Round up to ensure we use at least 1 movie per query
            int currentQueryQuota = (int) Math.ceil((double) remainingLimit / remainingQueries);
            log.info("Current query quota: {} (remaining limit: {}, remaining queries: {} ----- current params: {})",
                    currentQueryQuota, remainingLimit, remainingQueries, searchParams);

            List<Movie> results = tmdbService.searchMovies(searchParams);
            apiCallCount++;

            // Track how many movies we've added from this query
            int moviesAddedFromCurrentQuery = 0;

            // Filter out excluded movies and add to suggestions, but only up to the current query's quota
            for (Movie movie : results) {
                if (!excludedMovieIds.contains(movie.getMovieId()) &&
                        moviesAddedFromCurrentQuery < currentQueryQuota &&
                        !suggestions.contains(movie)) {

                    suggestions.add(movie);
                    log.info("Added movie {} to suggestions", movie.getTitle());
                    moviesAddedFromCurrentQuery++;

                    if (suggestions.size() >= limit) {
                        break;
                    }
                }
            }

            // Update remaining limit and queries for next iteration
            remainingLimit = limit - suggestions.size();
            remainingQueries--;

            log.info("Added {} movies from current query. Total suggestions so far: {}",
                    moviesAddedFromCurrentQuery, suggestions.size());
        }

        // If we still don't have enough suggestions, try a search with empty params
        if (suggestions.size() < limit && apiCallCount < MAX_API_CALLS) {
            Movie emptySearch = new Movie();
            log.info("Count of suggested Movies before empty search {}", suggestions.size());
            List<Movie> results = tmdbService.searchMovies(emptySearch);

            apiCallCount++;

            for (Movie movie : results) {
                if (!excludedMovieIds.contains(movie.getMovieId())) {
                    suggestions.add(movie);

                    if (suggestions.size() >= limit) {
                        log.info("Count of suggested Movies including empty search {}", suggestions.size());
                        break;
                    }
                }
            }
        }

        log.info("Generated {} movie suggestions for user {} using {} API calls",
                suggestions.size(), userId, apiCallCount);

        // Convert to list and return
        return new ArrayList<>(suggestions);
    }

    /**
     * Generate search parameter permutations ordered by specificity,
     * prioritizing directors over actors, and actors over genres
     *
     * @param genres List of genre names
     * @param actors List of actor IDs
     * @param directors List of director IDs
     * @return List of Movie objects with search parameters
     */
    private List<Movie> generateSearchPermutations(List<String> genres, List<String> actors, List<String> directors) {
        List<Movie> searchQueries = new ArrayList<>();

        // 1. First search query - intersection of all directors, actors, and genres
        if (!directors.isEmpty() && !actors.isEmpty() && !genres.isEmpty()) {
            Movie allCriteria = new Movie();
            allCriteria.setDirectors(directors);
            allCriteria.setActors(actors);
            allCriteria.setGenres(genres);
            searchQueries.add(allCriteria);
        }

        // 2. All genres with intersection of all directors and actors
        if (!directors.isEmpty() && !actors.isEmpty() && !genres.isEmpty()) {
            for (String genre : genres) {
                Movie query = new Movie();
                query.setDirectors(directors);
                query.setActors(actors);
                query.setGenres(Collections.singletonList(genre));
                searchQueries.add(query);
            }
        }

        // 3. All actors with intersection of all directors
        if (!directors.isEmpty() && !actors.isEmpty()) {
            for (String actor : actors) {
                Movie query = new Movie();
                query.setDirectors(directors);
                query.setActors(Collections.singletonList(actor));
                searchQueries.add(query);
            }
        }

        // 4. All directors individually
        if (!directors.isEmpty()) {
            for (String director : directors) {
                Movie query = new Movie();
                query.setDirectors(Collections.singletonList(director));
                searchQueries.add(query);
            }
        }

        // 5. All genres with intersection of all actors (no directors)
        if (!actors.isEmpty() && !genres.isEmpty()) {
            for (String genre : genres) {
                Movie query = new Movie();
                query.setActors(actors);
                query.setGenres(Collections.singletonList(genre));
                searchQueries.add(query);
            }
        }

        // 6. All actors individually (no directors)
        if (!actors.isEmpty()) {
            for (String actor : actors) {
                Movie query = new Movie();
                query.setActors(Collections.singletonList(actor));
                searchQueries.add(query);
            }
        }

        // 7. All genres individually
        if (!genres.isEmpty()) {
            for (String genre : genres) {
                Movie query = new Movie();
                query.setGenres(Collections.singletonList(genre));
                searchQueries.add(query);
            }
        }

        return searchQueries;
    }


    /**
     * Helper method to find actor IDs by actor names using the /movies/actors endpoint
     *
     * @param favoriteActorNames List of actor names to look up
     * @return List of actor IDs corresponding to the names
     */
    private List<String> HelperMethodFindActorIds(List<String> favoriteActorNames) {
        List<String> favoriteActorIds = new ArrayList<>();
        if (favoriteActorNames == null || favoriteActorNames.isEmpty()) {
            return favoriteActorIds;
        }

        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "https://sopra-fs25-group-19-server.oa.r.appspot.com";

        for (String favoriteactorName : favoriteActorNames) {
            log.info("actorName is {}", favoriteactorName);
            try {

                String url = baseUrl + "/movies/actors?actorname=" + favoriteactorName;
                log.info("url is {}", url);

                // Make the API call
                ResponseEntity<ActorDTO[]> response = restTemplate.getForEntity(url, ActorDTO[].class);
                log.info("response is {}", response);
                ActorDTO[] actors = response.getBody();

                if (actors != null && actors.length > 0) {
                    // First try to find exact match
                    boolean exactMatchFound = false;
                    for (ActorDTO actor : actors) {
                        if (actor.getActorName().equalsIgnoreCase(favoriteactorName)) {
                            favoriteActorIds.add(String.valueOf(actor.getActorId()));
                            exactMatchFound = true;
                            break;
                        }
                    }

                    // If no exact match, take the first result
                    if (!exactMatchFound && actors.length > 0) {
                        favoriteActorIds.add(String.valueOf(actors[0].getActorId()));
                    }
                }

                log.info("Found actor ID for {}: {}", favoriteactorName,
                        favoriteActorIds.isEmpty() ? "No match found" : favoriteActorIds.get(favoriteActorIds.size() - 1));

            } catch (Exception e) {
                log.error("Error finding actor ID for {}: {}", favoriteactorName, e.getMessage());
            }
        }

        return favoriteActorIds;
    }

    /**
     * Helper method to find director IDs by director names using the /movies/directors endpoint
     *
     * @param favoriteDirectorNames List of director names to look up
     * @return List of director IDs corresponding to the names
     */
    private List<String> HelperMethodFindDirectorIds(List<String> favoriteDirectorNames) {
        List<String> favoriteDirectorIds = new ArrayList<>();
        if (favoriteDirectorNames == null || favoriteDirectorNames.isEmpty()) {
            return favoriteDirectorIds;
        }

        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "https://sopra-fs25-group-19-server.oa.r.appspot.com";

        for (String directorName : favoriteDirectorNames) {
            try {
                String url = baseUrl + "/movies/directors?directorname=" + directorName;

                // Make the API call
                ResponseEntity<DirectorDTO[]> response = restTemplate.getForEntity(url, DirectorDTO[].class);
                DirectorDTO[] directors = response.getBody();

                if (directors != null && directors.length > 0) {
                    // First try to find exact match
                    boolean exactMatchFound = false;
                    for (DirectorDTO director : directors) {
                        if (director.getDirectorName().equalsIgnoreCase(directorName)) {
                            favoriteDirectorIds.add(String.valueOf(director.getDirectorId()));
                            exactMatchFound = true;
                            break;
                        }
                    }

                    // If no exact match, take the first result
                    if (!exactMatchFound && directors.length > 0) {
                        favoriteDirectorIds.add(String.valueOf(directors[0].getDirectorId()));
                    }
                }

                log.info("Found director ID for {}: {}", directorName,
                        favoriteDirectorIds.isEmpty() ? "No match found" : favoriteDirectorIds.get(favoriteDirectorIds.size() - 1));

            } catch (Exception e) {
                log.error("Error finding director ID for {}: {}", directorName, e.getMessage());
            }
        }
        return favoriteDirectorIds;
    }
}
