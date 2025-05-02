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
import java.util.stream.Collectors;

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
    private static final int DEFAULT_SUGGESTION_LIMIT = 100; //


    MovieController(MovieService movieService, TMDbService tmdbService) {
        this.movieService = movieService;
        this.tmdbService = tmdbService;
    }

    /**
     * Validates the search parameters
     *
     * @param title Search term for movie title
     * @param genres Genre to filter by
     * @param year Release year to filter by
     * @param actors Actor name to filter by
     * @param directors Director name to filter by
     * @throws SearchValidationException if any parameter is invalid
     */
    private void validateSearchParams(String title, List<String> genres, Integer year,
                                      List<String> actors, List<String> directors) {
        // Check if at least one parameter is provided
        boolean isTitleBlank = (title == null || title.isBlank());
        boolean isGenreEmpty = (genres == null || genres.stream().allMatch(item -> item == null || item.trim().isEmpty()));
        boolean isYearNull = (year == null);
        boolean isActorsEmpty = (actors == null || actors.stream().allMatch(item -> item == null || item.trim().isEmpty()));
        boolean isDirectorsEmpty = (directors == null || directors.stream().allMatch(item -> item == null || item.trim().isEmpty()));

        if (isTitleBlank && isGenreEmpty && isYearNull && isActorsEmpty && isDirectorsEmpty) {
            throw new SearchValidationException("At least one search parameter must be provided");
        }

        if (title != null && title.length() < MIN_SEARCH_TERM_LENGTH) {
            throw new SearchValidationException("Search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
        }

        //Validate genre if provided
        if (genres != null && !genres.isEmpty()) {
            try {
                // Set of valid genre names
                Set<String> validGenreNames = Set.of(
                        "Action", "Adventure", "Animation", "Comedy", "Crime",
                        "Documentary", "Drama", "Family", "Fantasy", "History",
                        "Horror", "Music", "Mystery", "Romance", "Science Fiction",
                        "TV Movie", "Thriller", "War", "Western"
                );

                // Validate each genre in the list
                for (String genre : genres) {
                    if (genre == null) {
                        throw new SearchValidationException("Genre cannot be null");
                    }

                    String trimmedGenre = genre.trim();
                    if (trimmedGenre.isEmpty()) {
                        throw new SearchValidationException("Genre cannot be empty");
                    }

                    if (!validGenreNames.contains(trimmedGenre)) {
                        throw new SearchValidationException("Invalid genre: " + trimmedGenre);
                    }
                }
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

        if (actors != null) {
            for (String actor : actors) {
                if (actor != null && !actor.trim().isEmpty() && actor.trim().length() < MIN_SEARCH_TERM_LENGTH) {
                    throw new SearchValidationException("Actor search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
                }
            }
        }

        if (directors != null) {
            for (String director : directors) {
                if (director != null && !director.trim().isEmpty() && director.trim().length() < MIN_SEARCH_TERM_LENGTH) {
                    throw new SearchValidationException("Director search term must be at least " + MIN_SEARCH_TERM_LENGTH + " characters long");
                }
            }
        }
    }

    /**
     * Search for movies based on various criteria using query parameters
     */
    @GetMapping("/movies")
    @ResponseStatus(HttpStatus.OK)
    public List<MovieGetDTO> getMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<String> actors,
            @RequestParam(required = false) List<String> directors,
            @RequestParam(required = false, defaultValue = "1") Integer page) {

        // Validate search parameters
        validateSearchParams(title, genres, year, actors, directors);


        // Create a movie object with search parameters
        Movie searchParams = new Movie();
        searchParams.setTitle(title);
        searchParams.setGenres(genres);
        searchParams.setYear(year);
        searchParams.setActors(actors);
        searchParams.setDirectors(directors);

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
    public MovieGetDTO getMovieById(@PathVariable long movieId) {
        Movie movie = movieService.getMovieById(movieId);
        return DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie);
    }

    @GetMapping("/movies/genres")
    @ResponseStatus(HttpStatus.OK)
    public JsonNode getGenres() {
        JsonNode genres = tmdbService.getGenres();
        if (genres == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not fetch genres from TMDB");
        }
        return genres;
    }

    /**
     * Get personalized movie suggestions for a user
     * New endpoint to retrieve personalized movie suggestions based on user favorites
     *
     * @param userId The user ID for which to generate suggestions
     * @return List of suggested movies
     */
    @GetMapping("/movies/suggestions/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<MovieGetDTO> getMovieSuggestions(@PathVariable Long userId) {
        // Get movie suggestions from service
        List<Movie> suggestions = movieService.getMovieSuggestions(userId, DEFAULT_SUGGESTION_LIMIT);

        // Convert to simplified DTO format with only essential information
        return suggestions.stream()
                .map(movie -> {
                    MovieGetDTO dto = new MovieGetDTO();
                    dto.setMovieId(movie.getMovieId());
                    dto.setTitle(movie.getTitle());
                    dto.setPosterURL(movie.getPosterURL());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
