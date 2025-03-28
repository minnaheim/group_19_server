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

import java.util.List;
import java.util.Optional;

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

    @Autowired
    public MovieService(@Qualifier("movieRepository") MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Get movies based on search criteria
     *
     * @param searchParams Movie object containing search parameters
     * @return List of movies matching the search criteria
     */
    public List<Movie> getMovies(Movie searchParams) {
        log.debug("Searching for movies with parameters: {}", searchParams);

        List<Movie> movies = movieRepository.findBySearchParams(
                searchParams.getTitle(),
                searchParams.getGenre(),
                searchParams.getYear(),
                searchParams.getCountry(),
                searchParams.getActor(),
                searchParams.getLanguage(),
                searchParams.getTrailerURL()
        );

        return movies;
    }

    /**
     * Get a specific movie by its ID
     *
     * @param movieId The movie's unique identifier
     * @return The movie if found
     * @throws ResponseStatusException if the movie does not exist
     */
    public Movie getMovieById(int movieId) {
        log.debug("Getting movie with ID: {}", movieId);

        Optional<Movie> movieOptional = movieRepository.findById(movieId);

        if (movieOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Movie with ID %d was not found", movieId));
        }

        return movieOptional.get();
    }
}