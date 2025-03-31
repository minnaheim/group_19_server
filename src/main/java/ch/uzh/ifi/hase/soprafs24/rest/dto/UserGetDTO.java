package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Movie;

public class UserGetDTO {

  private Long userId;
  private String name;
  private String username;
  private UserStatus status;
  private String bio;
  private List<String> favoriteGenres;
  private List<String> favoriteActors;
  private List<String> favoriteDirectors;
  private List<Movie> watchlist;
  private List<Movie> watchedMovies;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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