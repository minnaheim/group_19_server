package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Movie;
import ch.uzh.ifi.hase.soprafs24.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Autowired
    public MovieService(@Qualifier("movieRepository") MovieRepository movieRepository, TMDbService tmdbService) {
        this.movieRepository = movieRepository;
        this.tmdbService = tmdbService;
    }

    /**
     * Get movies based on search criteria from both local database and TMDb API
     *
     * @param searchParams Movie object containing search parameters
     * @return List of movies matching the search criteria without duplicates
     */
    public List<Movie> getMovies(Movie searchParams) {
        // First, search locally
        List<Movie> localMovies = movieRepository.findBySearchParams(
                searchParams.getTitle(),
                searchParams.getGenre(),
                searchParams.getYear(),
                searchParams.getCountry(),
                searchParams.getActor(),
                searchParams.getLanguage(),
                searchParams.getTrailerURL());

        // If we don't have any search parameters, just return local results
        if (!hasAnySearchParam(searchParams)) {
            return localMovies;
        }

        // Search from TMDb API
        List<Movie> tmdbMovies = tmdbService.searchMovies(searchParams);

        // Combine results, avoiding duplicates
        Map<Long, Movie> moviesMap = new HashMap<>();

        // Add all local movies to the map, keyed by movieId
        for (Movie movie : localMovies) {
            moviesMap.put(movie.getMovieId(), movie);
        }

        // Add TMDb movies to the map, but don't overwrite local movies with the same ID
        for (Movie movie : tmdbMovies) {
            if (!moviesMap.containsKey(movie.getMovieId())) {
                moviesMap.put(movie.getMovieId(), movie);
            }
        }

        return new ArrayList<>(moviesMap.values());
    }

    /**
     * Helper method to check if there are any search parameters provided
     */
    private boolean hasAnySearchParam(Movie searchParams) {
        return searchParams.getTitle() != null ||
                searchParams.getGenre() != null ||
                searchParams.getYear() != null ||
                searchParams.getCountry() != null ||
                searchParams.getActor() != null ||
                searchParams.getLanguage() != null ||
                searchParams.getTrailerURL() != null;
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
        // First check if movie exists locally
        Movie movie = movieRepository.findByMovieId(movieId);

        // If not found locally, try to get from TMDb
        if (movie == null) {
            movie = tmdbService.getMovieDetails(movieId);

            // If still not found, throw exception
            if (movie == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found with ID: " + movieId);
            }
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
        // Check if movie already exists in local DB
        Movie existingMovie = movieRepository.findByMovieId(movie.getMovieId());
        if (existingMovie != null) {
            return existingMovie; // Return existing movie to avoid duplicates
        }

        // If movie doesn't exist in local DB, save it
        return movieRepository.save(movie);
    }
}