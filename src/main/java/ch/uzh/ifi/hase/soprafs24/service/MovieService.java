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
import java.util.Optional;
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
        log.debug("Searching for movies with parameters: {}", searchParams);

        // Get movies from local database
        List<Movie> localMovies = movieRepository.findBySearchParams(
                searchParams.getTitle(),
                searchParams.getGenre(),
                searchParams.getYear(),
                searchParams.getCountry(),
                searchParams.getActor(),
                searchParams.getLanguage(),
                searchParams.getTrailerURL()
        );

        // Use a map to track movies by their TMDb ID to avoid duplicates
        Map<Integer, Movie> moviesMap = new HashMap<>();

        // Add local movies to the map
        for (Movie movie : localMovies) {
            moviesMap.put(movie.getMovieId(), movie);
        }

        // Only search TMDb if a title is provided (to avoid too broad searches)
        if (searchParams.getTitle() != null && !searchParams.getTitle().isEmpty()) {
            // Get movies from TMDb API
            List<Movie> tmdbMovies = tmdbService.searchMovies(searchParams.getTitle(), searchParams.getYear());

            // Add TMDb movies to the map (if not already present)
            for (Movie movie : tmdbMovies) {
                if (!moviesMap.containsKey(movie.getMovieId())) {
                    moviesMap.put(movie.getMovieId(), movie);
                }
            }
        }

        // Filter results based on other search parameters
        List<Movie> filteredMovies = new ArrayList<>(moviesMap.values());

        if (searchParams.getGenre() != null && !searchParams.getGenre().isEmpty()) {
            filteredMovies = filteredMovies.stream()
                    .filter(movie -> movie.getGenre() != null && movie.getGenre().toLowerCase().contains(searchParams.getGenre().toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (searchParams.getActor() != null && !searchParams.getActor().isEmpty()) {
            filteredMovies = filteredMovies.stream()
                    .filter(movie -> movie.getActor() != null && movie.getActor().toLowerCase().contains(searchParams.getActor().toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (searchParams.getCountry() != null && !searchParams.getCountry().isEmpty()) {
            filteredMovies = filteredMovies.stream()
                    .filter(movie -> movie.getCountry() != null && movie.getCountry().toLowerCase().contains(searchParams.getCountry().toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (searchParams.getLanguage() != null && !searchParams.getLanguage().isEmpty()) {
            filteredMovies = filteredMovies.stream()
                    .filter(movie -> movie.getLanguage() != null && movie.getLanguage().toLowerCase().contains(searchParams.getLanguage().toLowerCase()))
                    .collect(Collectors.toList());
        }

        return filteredMovies;
    }

    /**
     * Get a specific movie by its ID
     * First tries to get from local DB, then falls back to TMDb
     *
     * @param movieId The movie's unique identifier (TMDb ID)
     * @return The movie if found
     * @throws ResponseStatusException if the movie does not exist
     */
    public Movie getMovieById(int movieId) {
        log.debug("Getting movie with ID: {}", movieId);

        // Try to get the movie from local database first
        Optional<Movie> movieOptional = movieRepository.findById(movieId);

        if (movieOptional.isPresent()) {
            return movieOptional.get();
        }

        // If not in local DB, try to get from TMDb
        Movie tmdbMovie = tmdbService.getMovieDetails(movieId);

        if (tmdbMovie == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Movie with ID %d was not found", movieId));
        }

        return tmdbMovie;
    }

    /**
     * Saves a movie to the local database
     * Use this method for movies that need to be persisted (e.g., favorites)
     *
     * @param movie The movie to save
     * @return The saved movie
     */
    public Movie saveMovie(Movie movie) {
        log.debug("Saving movie with ID: {}", movie.getMovieId());

        // Check if movie already exists in local DB
        Optional<Movie> existingMovie = movieRepository.findById(movie.getMovieId());

        if (existingMovie.isPresent()) {
            return existingMovie.get();
        }

        // If movie doesn't exist in local DB but is missing some details, fetch from TMDb
        if (movie.getDescription() == null || movie.getPosterURL() == null) {
            Movie tmdbMovie = tmdbService.getMovieDetails(movie.getMovieId());
            if (tmdbMovie != null) {
                // Update missing fields from TMDb data
                if (movie.getDescription() == null) movie.setDescription(tmdbMovie.getDescription());
                if (movie.getPosterURL() == null) movie.setPosterURL(tmdbMovie.getPosterURL());
                if (movie.getTrailerURL() == null) movie.setTrailerURL(tmdbMovie.getTrailerURL());
                if (movie.getGenre() == null) movie.setGenre(tmdbMovie.getGenre());
                if (movie.getActor() == null) movie.setActor(tmdbMovie.getActor());
                if (movie.getYear() == null) movie.setYear(tmdbMovie.getYear());
                if (movie.getLanguage() == null) movie.setLanguage(tmdbMovie.getLanguage());
            }
        }

        return movieRepository.save(movie);
    }
}