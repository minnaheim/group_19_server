package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
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
        // Check if any search parameters are provided
        if (!hasAnySearchParam(searchParams)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one search parameter must be provided");
        }

        // First, search locally
        List<Movie> localMovies = movieRepository.findBySearchParams(
                searchParams.getTitle(),
                searchParams.getGenre(),
                searchParams.getYear(),
                searchParams.getActor().isEmpty() ? null : searchParams.getActor().get(0),
                searchParams.getDirector().isEmpty() ? null : searchParams.getDirector().get(0)
        );

        // Search in TMDb API
        List<Movie> tmdbMovies = tmdbService.searchMovies(searchParams);

        // Combine results and remove duplicates
        Map<Long, Movie> uniqueMovies = new HashMap<>();

        // Add local movies first
        for (Movie movie : localMovies) {
            uniqueMovies.put(movie.getMovieId(), movie);
        }

        // Add TMDb movies (will override local movies with same ID)
        for (Movie movie : tmdbMovies) {
            uniqueMovies.put(movie.getMovieId(), movie);
        }

        return new ArrayList<>(uniqueMovies.values());
    }

    /**
     * Helper method to check if there are any search parameters provided
     */
    private boolean hasAnySearchParam(Movie searchParams) {
        return (searchParams.getTitle() != null && !searchParams.getTitle().isBlank()) ||
                (searchParams.getGenre() != null && !searchParams.getGenre().isBlank()) ||
                searchParams.getYear() != null ||
                (searchParams.getActor() != null && !searchParams.getActor().isEmpty()) ||
                (searchParams.getDirector() != null && !searchParams.getDirector().isEmpty());
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
                log.error("Failed to get movie from TMDb", e);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found");
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
        // Set required fields if not set
        if (movie.getMovieId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movie ID is required");
        }

        // Check if movie already exists
        Movie existingMovie = movieRepository.findByMovieId(movie.getMovieId());
        if (existingMovie != null) {
            return existingMovie; // Return existing movie to avoid duplicates
        }

        // Save the movie
        return movieRepository.save(movie);
    }
}