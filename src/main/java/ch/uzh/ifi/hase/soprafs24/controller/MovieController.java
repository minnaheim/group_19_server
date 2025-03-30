package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Movie;
import ch.uzh.ifi.hase.soprafs24.exceptions.SearchValidationException;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MovieService;
import ch.uzh.ifi.hase.soprafs24.service.TMDbService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;


/**
 * Movie Controller
 * This class is responsible for handling all REST request that are related to
 * movie search and retrieval.
 */
@RestController
public class MovieController {

    private final MovieService movieService;
    private final TMDbService tmdbService;
    private static final int CURRENT_YEAR = LocalDate.now().getYear();
    private static final int MIN_MOVIE_YEAR = 1888; // First movie ever made
    private static final int MIN_SEARCH_TERM_LENGTH = 2;

    MovieController(MovieService movieService, TMDbService tmdbService) {
        this.movieService = movieService;
        this.tmdbService = tmdbService;
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
        // Check if at least one parameter is provided
        if (title == null && genre == null && year == null && country == null && actor == null && language == null) {
            throw new SearchValidationException("At least one search parameter is required");
        }

        if (title != null && title.length() < MIN_SEARCH_TERM_LENGTH) {
            throw new SearchValidationException("Search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        if (year != null && (year < MIN_MOVIE_YEAR || year > CURRENT_YEAR + 10)) {
            throw new SearchValidationException("Year must be between " + MIN_MOVIE_YEAR + " and " + (CURRENT_YEAR + 10));
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


        //Validate genre if provided
        if (genre != null) {
            try {
                // Get the set of valid genre IDs from the known list
                Set<String> validGenreIds = Set.of(
                        "28", "12", "16", "35", "80", "99", "18", "10751", "14",
                        "36", "27", "10402", "9648", "10749", "878", "10770", "53", "10752", "37"
                );

                // Handle comma-separated genre IDs
                String[] genreIds = genre.split(",\\s*");
                for (String genreId : genreIds) {
                    String trimmedId = genreId.trim();
                    if (!validGenreIds.contains(trimmedId)) {
                        throw new SearchValidationException("Invalid genre ID: " + trimmedId);
                    }
                }
            } catch (Exception e) {
                if (e instanceof SearchValidationException) {
                    throw e;
                }
                throw new SearchValidationException("Error validating genre: " + e.getMessage());
            }
        }


        // Check other text parameters for minimum length if provided
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
            @RequestParam(required = false) String trailerURL,
            @RequestParam(required = false, defaultValue = "1") Integer page) {

        // Validate search parameters
        validateSearchParams(title, genre, year, country, actor, language);


        // Create a movie object with search parameters
        Movie searchParams = new Movie();
        searchParams.setTitle(title);
        searchParams.setGenre(genre);
        searchParams.setYear(year);
        searchParams.setCountry(country);
        searchParams.setActor(actor);
        searchParams.setLanguage(language);
        searchParams.setTrailerURL(trailerURL);

        List<Movie> movies = movieService.getMovies(searchParams);

        // Check if no results were found
        if (movies.isEmpty()) {
            throw new SearchValidationException("No movies found matching the search criteria");
        }

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
        Movie movie = movieService.getMovieById(movieId);
        return DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie);
    }

    @GetMapping("/movies/genres")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public JsonNode getGenres() {
        JsonNode genres = tmdbService.getGenres();
        if (genres == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not fetch genres from TMDB");
        }
        return genres;
    }
}
