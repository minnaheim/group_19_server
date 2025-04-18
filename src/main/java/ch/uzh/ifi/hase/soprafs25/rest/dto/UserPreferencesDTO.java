package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;

public class UserPreferencesDTO {
    private List<String> favoriteGenres;
    private Movie favoriteMovie;

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(List<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }

    public Movie getFavoriteMovie() {
        return favoriteMovie;
    }

    public void setFavoriteMovie(Movie favoriteMovie) {
        this.favoriteMovie = favoriteMovie;
    }
}
