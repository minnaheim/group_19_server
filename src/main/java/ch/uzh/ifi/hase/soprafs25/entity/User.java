package ch.uzh.ifi.hase.soprafs25.entity;

import java.io.Serializable;
import java.util.*;
import java.util.Map;
import java.util.HashMap;
import javax.persistence.MapKeyColumn;

import javax.persistence.*;

import ch.uzh.ifi.hase.soprafs25.constant.UserStatus;

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
  private Long userId;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;
  
  @Column(unique = true)
  private String token;

  @Column(nullable = false)
  private UserStatus status;

  @Column
  private String bio;

  @ManyToOne
  @JoinColumn(name = "favorite_movie_id")
  private Movie favoriteMovie;

  @ElementCollection
  @CollectionTable(name = "user_favorite_genres", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "genre")
  private List<String> favoriteGenres;

  @ElementCollection
  @CollectionTable(name = "user_favorite_actors", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "actor")
  private Map<String, String> favoriteActors = new HashMap<>();

  @ElementCollection
  @CollectionTable(name = "user_favorite_directors", joinColumns = @JoinColumn(name = "user_id"))
  @MapKeyColumn(name = "directorId")
  @Column(name = "director_name")
  private Map<String, String> favoriteDirectors = new HashMap<>();


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

//  dealing with friends
//  special separate table with users and their friends

  @ManyToMany
  @JoinTable(
    name = "USER_FRIENDS",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "friend_id")
  )
  private Set<User> friends = new HashSet<>();
  
//   for sent requests
  @OneToMany(mappedBy = "sender")
  private Set<FriendRequest> sentFriendRequests = new HashSet<>();
// for received
  @OneToMany(mappedBy = "receiver")
  private Set<FriendRequest> receivedFriendRequests = new HashSet<>();

  
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

    public Set<User> getFriends() {
        return friends;
    }
  
    public void setFriends(Set<User> friends) {
        this.friends = friends;
    }

    public Set<FriendRequest> getSentFriendRequests() {
        return sentFriendRequests;
    }

    public void setSentFriendRequests(Set<FriendRequest> sentFriendRequests) {
        this.sentFriendRequests = sentFriendRequests;
    }

    public Set<FriendRequest> getReceivedFriendRequests() {
        return receivedFriendRequests;
    }

    public void setReceivedFriendRequests(Set<FriendRequest> receivedFriendRequests) {
        this.receivedFriendRequests = receivedFriendRequests;
    }
}
