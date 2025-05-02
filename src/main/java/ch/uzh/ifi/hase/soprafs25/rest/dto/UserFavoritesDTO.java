package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;

public class UserFavoritesDTO {
    private List<String> favoriteGenres;
    private Movie favoriteMovie;
    private List<String> favoriteActors;
    private List<String> favoriteDirectors;

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

    public List<String> getFavoriteActors() {
        return favoriteActors;
    }

    public void setFavoriteActors(List<String> favoriteActors) {
        this.favoriteActors = favoriteActors;
    }

    public List<String> getFavoriteDirectors() {
        return favoriteDirectors;
    }

    public void setFavoriteDirectors(List<String> favoriteDirectors) {
        this.favoriteDirectors = favoriteDirectors;
    }
}
