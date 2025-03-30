package ch.uzh.ifi.hase.soprafs24.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.*;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "USER")
public class User implements Serializable {

  // private static final long serialVersionUID = 1L;

  // user attributes
  @Id
  @GeneratedValue
  private int id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;
  
  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private UserStatus status;

  @Column
  private String bio;

  @ElementCollection
  @CollectionTable(name = "user_preferences", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "preference")
  private List<String> preferences;

  @ManyToMany
  @JoinTable(
          name = "user_watchlist",
          joinColumns = @JoinColumn(name = "user_id"),
          inverseJoinColumns = @JoinColumn(name = "movie_id")
  )
  private List<Movie> watchlist;

  @ManyToMany
  @JoinTable(
          name = "user_watched_movies",
          joinColumns = @JoinColumn(name = "user_id"),
          inverseJoinColumns = @JoinColumn(name = "movie_id")
  )
  private List<Movie> watchedMovies;
  ;

  public int getId() {
      return id;
  }

  public void setId(int id) {
      this.id = id;
  }

  public String getUsername() {
      return username;
  }

  public void setUsername(String username) {
      this.username = username;
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

  public String getToken() {
      return token;
  }

  public void setToken(String token) {
      this.token = token;
  }

  public UserStatus getStatus() {
      return status;
  }

  public void setStatus(UserStatus status) {
      this.status = status;
  }


}
