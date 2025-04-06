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
        List<Movie> results = new ArrayList<>();

        // Check if any search parameters are provided
        if (!hasAnySearchParam(searchParams)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one search parameter must be provided");
        }

        // Search local database
        List<Movie> localResults;

        localResults = movieRepository.findBySearchParamsWithLists(
                searchParams.getTitle(),
                searchParams.getGenre(),
                searchParams.getYear(),
                searchParams.getActors(),
                searchParams.getDirectors()
        );

        results.addAll(localResults);

        // Search in TMDb API
        try {
            List<Movie> tmdbResults = tmdbService.searchMovies(searchParams);

            // Add to results, avoiding duplicates
            Map<Long, Movie> movieMap = new HashMap<>();

            // Add local results to map
            for (Movie movie : results) {
                movieMap.put(movie.getMovieId(), movie);
            }

            // Add TMDb results to map, avoiding duplicates
            for (Movie movie : tmdbResults) {
                if (!movieMap.containsKey(movie.getMovieId())) {
                    movieMap.put(movie.getMovieId(), movie);
                    results.add(movie);
                }
            }
        } catch (Exception e) {
            log.error("Error searching TMDb: {}", e.getMessage());
            // Continue with just local results if TMDb search fails
        }

        return results;
    }


    /**
     * Helper method to check if there are any search parameters provided
     */
    private boolean hasAnySearchParam(Movie searchParams) {
        return (searchParams.getTitle() != null && !searchParams.getTitle().trim().isEmpty()) ||
                (searchParams.getGenre() != null && !searchParams.getGenre().trim().isEmpty()) ||
                searchParams.getYear() != null ||
                (searchParams.getActors() != null && !searchParams.getActors().isEmpty()) ||
                (searchParams.getDirectors() != null && !searchParams.getDirectors().isEmpty());
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

        // Check if movie already exists
        Movie existingMovie = movieRepository.findByMovieId(movie.getMovieId());
        if (existingMovie != null) {
            return existingMovie; // Return existing movie to avoid duplicates
        }

        // Save the movie
        return movieRepository.save(movie);
    }
}