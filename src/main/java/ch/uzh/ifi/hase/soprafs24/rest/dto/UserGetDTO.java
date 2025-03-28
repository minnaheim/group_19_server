package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Movie;

public class UserGetDTO {

  private int id;
  private String name;
  private String username;
  private UserStatus status;
  private String bio;
  private List<String> preferences;
  private List<Movie> watchlist;
  private List<Movie> watchedMovies;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  public List<String> getPreferences() {
    return preferences;
  }

  public void setPreferences(List<String> preferences) {
    this.preferences = preferences;
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
}