package ch.uzh.ifi.hase.soprafs25.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.repository.MovieRepository;
import java.util.ArrayList;

@Service
public class MoviePersistenceService {

    @Autowired
    private MovieRepository movieRepository;

    @Transactional
    public Movie saveOrGet(Movie movie) {
        // Check if movie already exists to avoid duplicate-key errors
        Movie existing = movieRepository.findByMovieId(movie.getMovieId());
        if (existing != null) {
            return existing;
        }
        // Initialize collections for new movie to avoid null element collections
        if (movie.getGenres() == null) movie.setGenres(new ArrayList<>());
        if (movie.getActors() == null) movie.setActors(new ArrayList<>());
        if (movie.getDirectors() == null) movie.setDirectors(new ArrayList<>());
        if (movie.getSpokenlanguages() == null) movie.setSpokenlanguages(new ArrayList<>());
        return movieRepository.save(movie);
    }
}
