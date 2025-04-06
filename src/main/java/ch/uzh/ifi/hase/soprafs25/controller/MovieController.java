package ch.uzh.ifi.hase.soprafs25.controller;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.exceptions.SearchValidationException;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs25.service.MovieService;
import ch.uzh.ifi.hase.soprafs25.service.TMDbService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;


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
    private static final int MIN_SEARCH_TERM_LENGTH = 1;

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
     * @param actor Actor name to filter by
     * @param director Director name to filter by
     * @throws SearchValidationException if any parameter is invalid
     */
    private void validateSearchParams(String title, String genre, Integer year,
                                      String actor, String director) {
        // Check if at least one parameter is provided
        if ((title == null || title.isBlank()) &&
                (genre == null || genre.isBlank()) &&
                year == null &&
                (actor == null || actor.isBlank()) &&
                (director == null || director.isBlank())) {
            throw new SearchValidationException("At least one search parameter must be provided");
        }

        if (title != null && title.length() < MIN_SEARCH_TERM_LENGTH) {
            throw new SearchValidationException("Search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        //Validate genre if provided
        if (genre != null) {
            try {
                // Map of genre names to genre IDs
                Map<String, String> genreNameToId = Map.ofEntries(
                        Map.entry("Action", "28"),
                        Map.entry("Adventure", "12"),
                        Map.entry("Animation", "16"),
                        Map.entry("Comedy", "35"),
                        Map.entry("Crime", "80"),
                        Map.entry("Documentary", "99"),
                        Map.entry("Drama", "18"),
                        Map.entry("Family", "10751"),
                        Map.entry("Fantasy", "14"),
                        Map.entry("History", "36"),
                        Map.entry("Horror", "27"),
                        Map.entry("Music", "10402"),
                        Map.entry("Mystery", "9648"),
                        Map.entry("Romance", "10749"),
                        Map.entry("Science Fiction", "878"),
                        Map.entry("TV Movie", "10770"),
                        Map.entry("Thriller", "53"),
                        Map.entry("War", "10752"),
                        Map.entry("Western", "37")
                );

                // Get the set of valid genre IDs
                Set<String> validGenreIds = new HashSet<>(genreNameToId.values());

                // Handle comma-separated genres (could be IDs or names)
                String[] genres = genre.split(",\\s*");
                for (int i = 0; i < genres.length; i++) {
                    String input = genres[i].trim();

                    // Check if the input is a valid genre ID
                    if (validGenreIds.contains(input)) {
                        continue; // Valid genre ID, nothing to convert
                    }

                    // Check if the input is a valid genre name
                    String genreId = genreNameToId.get(input);
                    if (genreId != null) {
                        // Replace the genre name with its corresponding ID
                        genres[i] = genreId;
                    } else {
                        throw new SearchValidationException("Invalid genre: " + input);
                    }
                }

                // Join the genres back together (now all as IDs)
                genre = String.join(",", genres);

            } catch (Exception e) {
                if (e instanceof SearchValidationException) {
                    throw e;
                }
                throw new SearchValidationException("Error validating genre: " + e.getMessage());
            }
        }

        if (year != null && (year < MIN_MOVIE_YEAR || year > CURRENT_YEAR + 10)) {
            throw new SearchValidationException("Year must be between " + MIN_MOVIE_YEAR + " and " + (CURRENT_YEAR + 10)); // +10 to include Pre-production announcements
        }

        if (actor != null && actor.trim().length() < MIN_SEARCH_TERM_LENGTH && !actor.trim().isEmpty()) {
            throw new SearchValidationException("Actor search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        if (director != null && director.trim().length() < MIN_SEARCH_TERM_LENGTH && !director.trim().isEmpty()) {
            throw new SearchValidationException("Director search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
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
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String director,
            @RequestParam(required = false, defaultValue = "1") Integer page) {

        // Validate search parameters
        validateSearchParams(title, genre, year, actor, director);


        // Create a movie object with search parameters
        Movie searchParams = new Movie();
        searchParams.setTitle(title);
        searchParams.setGenre(genre);
        searchParams.setYear(year);
        searchParams.setActor(actor);
        searchParams.setActor(director);

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
