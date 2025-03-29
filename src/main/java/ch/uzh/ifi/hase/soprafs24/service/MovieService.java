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
        // Get movies from local database based on search parameters
        List<Movie> localMovies = movieRepository.findBySearchParams(
                searchParams.getTitle(),
                searchParams.getGenre(),
                searchParams.getYear(),
                searchParams.getCountry(),
                searchParams.getActor(),
                searchParams.getLanguage(),
                searchParams.getTrailerURL());

        // Check if any search parameters were provided
        boolean hasSearchParams = hasAnySearchParam(searchParams);

        // If no search parameters, just return local movies (avoid fetching everything from TMDb)
        if (!hasSearchParams) {
            return localMovies;
        }

        // Get movies from TMDb API
        List<Movie> tmdbMovies = tmdbService.searchMovies(searchParams);

        // Remove duplicates (movies already in local DB)
        Map<Long, Movie> moviesMap = new HashMap<>();

        // Add local movies first
        for (Movie movie : localMovies) {
            moviesMap.put(movie.getMovieId(), movie);
        }

        // Add TMDb movies, only if they don't exist in local DB
        for (Movie movie : tmdbMovies) {
            if (!moviesMap.containsKey(movie.getMovieId())) {
                moviesMap.put(movie.getMovieId(), movie);
            }
        }

        // Return combined list without duplicates
        return new ArrayList<>(moviesMap.values());
    }

    /**
     * Helper method to check if there are any search parameters provided
     */
    private boolean hasAnySearchParam(Movie searchParams) {
        return (searchParams.getTitle() != null && !searchParams.getTitle().isEmpty()) ||
                (searchParams.getGenre() != null && !searchParams.getGenre().isEmpty()) ||
                (searchParams.getYear() != null) ||
                (searchParams.getCountry() != null && !searchParams.getCountry().isEmpty()) ||
                (searchParams.getActor() != null && !searchParams.getActor().isEmpty()) ||
                (searchParams.getLanguage() != null && !searchParams.getLanguage().isEmpty()) ||
                (searchParams.getTrailerURL() != null && !searchParams.getTrailerURL().isEmpty());
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
        // First check if the movie exists in our local database
        Movie movie = movieRepository.findByMovieId(movieId);

        // If not found in local DB, try to get from TMDb API
        if (movie == null) {
            movie = tmdbService.getMovieDetails(movieId);

            // If still not found, throw exception
            if (movie == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Movie with ID %d was not found", movieId));
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