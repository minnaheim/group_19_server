package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Movie;
import ch.uzh.ifi.hase.soprafs24.exceptions.SearchValidationException;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Movie Controller
 * This class is responsible for handling all REST request that are related to
 * movie search and retrieval.
 */
@RestController
public class MovieController {

    private final MovieService movieService;
    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final int MIN_MOVIE_YEAR = 1888; // First movie ever made
    private static final int MIN_SEARCH_TERM_LENGTH = 2;

    MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Validates the search parameters
     *
     * @param title Search term for movie title
     * @param genre Genre to filter by
     * @param year Release year to filter by
     * @param country Country to filter by
     * @param actor Actor name to filter by
     * @param language Language to filter by
     * @throws SearchValidationException if any parameter is invalid
     */
    private void validateSearchParams(String title, String genre, Integer year,
                                      String country, String actor, String language) {
        // Check for minimum search term length
        if (title != null && title.trim().length() < MIN_SEARCH_TERM_LENGTH && !title.trim().isEmpty()) {
            throw new SearchValidationException("Search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        // Check year range
        if (year != null) {
            if (year < MIN_MOVIE_YEAR) {
                throw new SearchValidationException("Year cannot be earlier than " + MIN_MOVIE_YEAR);
            }
            if (year > CURRENT_YEAR + 5) { // Allow for future movies up to 5 years ahead
                throw new SearchValidationException("Year cannot be later than " + (CURRENT_YEAR + 5));
            }
        }

        // Check other text parameters for minimum length if provided
        if (genre != null && genre.trim().length() < MIN_SEARCH_TERM_LENGTH && !genre.trim().isEmpty()) {
            throw new SearchValidationException("Genre search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        if (country != null && country.trim().length() < MIN_SEARCH_TERM_LENGTH && !country.trim().isEmpty()) {
            throw new SearchValidationException("Country search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        if (actor != null && actor.trim().length() < MIN_SEARCH_TERM_LENGTH && !actor.trim().isEmpty()) {
            throw new SearchValidationException("Actor search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        if (language != null && language.trim().length() < MIN_SEARCH_TERM_LENGTH && !language.trim().isEmpty()) {
            throw new SearchValidationException("Language search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }
    }

    /**
     * Search for movies based on various criteria using query parameters
     */
    @GetMapping("/movies")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<MovieGetDTO> getMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String trailerURL) {

        // Validate search parameters
        validateSearchParams(title, genre, year, country, actor, language);

        // Create search params object
        Movie searchParams = new Movie();
        searchParams.setTitle(title);
        searchParams.setGenre(genre);
        searchParams.setYear(year);
        searchParams.setCountry(country);
        searchParams.setActor(actor);
        searchParams.setLanguage(language);
        searchParams.setTrailerURL(trailerURL);

        // Get movies from service
        List<Movie> movies = movieService.getMovies(searchParams);

        // Map to DTOs
        List<MovieGetDTO> movieGetDTOs = new ArrayList<>();
        for (Movie movie : movies) {
            movieGetDTOs.add(DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie));
        }

        return movieGetDTOs;
    }

    @GetMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public MovieGetDTO getMovieById(@PathVariable long movieId) {
        // Check for valid movie ID
        if (movieId <= 0) {
            throw new SearchValidationException("Invalid movie ID: must be a positive integer");
        }

        Movie movie = movieService.getMovieById(movieId);
        return DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie);
    }
}