package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Movie;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    MovieController(MovieService movieService) {
        this.movieService = movieService;
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

        // Create a MovieGetDTO and populate it with query parameters
        MovieGetDTO searchDTO = new MovieGetDTO();
        searchDTO.setTitle(title);
        searchDTO.setGenre(genre);
        searchDTO.setYear(year);
        searchDTO.setCountry(country);
        searchDTO.setActor(actor);
        searchDTO.setLanguage(language);
        searchDTO.setTrailerURL(trailerURL);

        // Convert search DTO to entity
        Movie searchParams = DTOMapper.INSTANCE.convertMovieGetDTOtoEntity(searchDTO);

        // Get matching movies
        List<Movie> movies = movieService.getMovies(searchParams);
        List<MovieGetDTO> movieGetDTOs = new ArrayList<>();

        // Convert entities to DTOs
        for (Movie movie : movies) {
            movieGetDTOs.add(DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie));
        }

        return movieGetDTOs;
    }


    @GetMapping("/movies/{movieId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public MovieGetDTO getMovieById(@PathVariable int movieId) {
        Movie movie = movieService.getMovieById(movieId);
        return DTOMapper.INSTANCE.convertEntityToMovieGetDTO(movie);
    }
}