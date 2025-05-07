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

                // Save to local DB for future queries
                if (movie != null) {
                    movieRepository.save(movie);
                }
                log.info("getActors size is? {}", movie.getActors().size());
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
        List<String> favoriteActorIds = new ArrayList<>();
        List<String> favoriteDirectorIds = new ArrayList<>();

        // Extract actor and director IDs
        if (user.getFavoriteActors() != null) {
            favoriteActorIds.addAll(user.getFavoriteActors());
        }
        log.info("getMovieSuggestions: Favorite actors are {}", favoriteActorIds);

        if (user.getFavoriteDirectors() != null) {
            favoriteDirectorIds.addAll(user.getFavoriteDirectors());
        }
        log.info("getMovieSuggestions: Favorite directors are {}", favoriteDirectorIds);

        // Maximum number of API calls to prevent excessive requests
        final int MAX_API_CALLS = 200;
        int apiCallCount = 0;

        // Resulting list of suggested movies
        Set<Movie> suggestions = new HashSet<>();

        // Generate all permutations of search parameters, starting with most specific
        List<Movie> searchQueries = generateSearchPermutations(favoriteGenres, favoriteActorIds, favoriteDirectorIds);

        log.info("Generated {} search permutations for user {}", searchQueries.size(), userId, searchQueries);

        // Execute searches in order until we have enough suggestions or reach API call limit
        for (Movie searchParams : searchQueries) {
            log.info("for (Movie searchParams : {}) loop", searchQueries);
            if (suggestions.size() >= limit || apiCallCount >= MAX_API_CALLS) {
                break;
            }

            List<Movie> results = tmdbService.searchMovies(searchParams);
            apiCallCount++;

            // Filter out excluded movies and add to suggestions
            for (Movie movie : results) {
                if (!excludedMovieIds.contains(movie.getMovieId())) {
                    suggestions.add(movie);

                    if (suggestions.size() >= limit) {
                        break;
                    }
                }
            }
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
     * Generate search parameter permutations ordered by specificity
     * helper method to generate search parameter permutations
     *
     * @param genres List of genre names
     * @param actors List of actor IDs
     * @param directors List of director IDs
     * @return List of Movie objects with search parameters
     */
    private List<Movie> generateSearchPermutations(List<String> genres, List<String> actors, List<String> directors) {
        List<Movie> searchQueries = new ArrayList<>();
        log.info("generateSearchPermutations: genres are {}, actors are {}, directors are {}", genres, actors, directors);

        // Start with the most specific search (all parameters)
        if (!genres.isEmpty() && !actors.isEmpty() && !directors.isEmpty()) {
            Movie fullSearch = new Movie();
            fullSearch.setGenres(new ArrayList<>(genres));
            fullSearch.setActors(actors.stream().collect(Collectors.toList()));
            fullSearch.setDirectors(directors.stream().collect(Collectors.toList()));
            searchQueries.add(fullSearch);
        }

        // Generate permutations with one parameter missing
        // Genres + Actors
        if (!genres.isEmpty() && !actors.isEmpty()) {
            Movie search = new Movie();
            search.setGenres(new ArrayList<>(genres));
            search.setActors(actors.stream().collect(Collectors.toList()));
            searchQueries.add(search);
        }

        // Genres + Directors
        if (!genres.isEmpty() && !directors.isEmpty()) {
            Movie search = new Movie();
            search.setGenres(new ArrayList<>(genres));
            search.setDirectors(directors.stream().collect(Collectors.toList()));
            searchQueries.add(search);
        }

        // Actors + Directors
        if (!actors.isEmpty() && !directors.isEmpty()) {
            Movie search = new Movie();
            search.setActors(actors.stream().collect(Collectors.toList()));
            search.setDirectors(directors.stream().collect(Collectors.toList()));
            searchQueries.add(search);
        }

        // Single parameter searches
        // Only Genres
        if (!genres.isEmpty()) {
            Movie search = new Movie();
            search.setGenres(new ArrayList<>(genres));
            searchQueries.add(search);
        }

        // Only Actors
        if (!actors.isEmpty()) {
            Movie search = new Movie();
            search.setActors(actors.stream().collect(Collectors.toList()));
            searchQueries.add(search);
        }

        // Only Directors
        if (!directors.isEmpty()) {
            Movie search = new Movie();
            search.setDirectors(directors.stream().collect(Collectors.toList()));
            searchQueries.add(search);
        }

        // Add individual genre searches
        for (String genre : genres) {
            Movie search = new Movie();
            search.setGenres(Collections.singletonList(genre));
            searchQueries.add(search);
        }

        // Add individual actor searches
        for (String actor : actors) {
            Movie search = new Movie();
            search.setActors(Collections.singletonList(actor));
            searchQueries.add(search);
        }

        // Add individual director searches
        for (String director : directors) {
            Movie search = new Movie();
            search.setDirectors(Collections.singletonList(director));
            searchQueries.add(search);
        }

        return searchQueries;
    }
}
