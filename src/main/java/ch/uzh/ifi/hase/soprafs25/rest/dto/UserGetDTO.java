package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.persistence.MapKeyColumn;


import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;

public class UserGetDTO {

  private Long userId;
  private String username;
  private String email; // Added email field
  private String password; // For client compatibility
  private UserStatus status;
  private String token;
  private String bio;
  private List<String> favoriteGenres;
  private Map<String, String> favoriteActors;
  private Map<String, String> favoriteDirectors;
  private Movie favoriteMovie;
  private List<Movie> watchlist;
  private List<Movie> watchedMovies;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getToken() {
      return token;
  }

  public void setToken(String token) {
      this.token = token;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public List<Movie> getWatchlist() {
    return watchlist;
  }

  public void setWatchlist(List<Movie> watchlist) {
    this.watchlist = watchlist;
  }

  public List<Movie> getWatchedMovies() {
    return watchedMovies;
  }

  public void setWatchedMovies(List<Movie> watchedMovies) {
    this.watchedMovies = watchedMovies;
  }

    public List<String> getFavoriteGenres() {
        return favoriteGenres;
    }

    public void setFavoriteGenres(List<String> favoriteGenres) {
        this.favoriteGenres = favoriteGenres;
    }

    public Map<String, String> getFavoriteActors() {
        return favoriteActors;
    }

    public void setFavoriteActors(Map<String, String> favoriteActors) {
        this.favoriteActors = favoriteActors;
    }

    public Map<String, String> getFavoriteDirectors() {
        return favoriteDirectors;
    }

    public void setFavoriteDirectors(Map<String, String> favoriteDirectors) {
        this.favoriteDirectors = favoriteDirectors;
    }
  
  
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
  
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
  
    public Movie getFavoriteMovie() {
        return favoriteMovie;
    }

    public void setFavoriteMovie(Movie favoriteMovie) {
        this.favoriteMovie = favoriteMovie;
    }
}
