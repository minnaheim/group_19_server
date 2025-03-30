package ch.uzh.ifi.hase.soprafs24.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Internal Movie Representation
 * This class composes the internal representation of the movie and defines how
 * the movie is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */

@Entity
@Table(name = "MOVIE")
public class Movie implements Serializable {

//   private static final long serialVersionUID = 1L;

  // movie attributes

//   if we generate id by ourselves
// @Id
//   @GeneratedValue

// if we use id from tmdb
  @Column(nullable=false, unique=true)
  private Long movieId;

  @Column
  private String title;

  @Column
  private String posterUrl;

  @Column
  private String details;

  @Column
  private String genre;

  @Column
  private String director;

  @Column
  private List<String> actors;

  @Column
  private String trailerURL;

//   getters and setters
  public long  getMovieId() {
    return movieId;
  }

  public void setMovieId(Long movieId) {
    this.movieId = movieId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public void setPosterUrl(String posterUrl) {
    this.posterUrl = posterUrl;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getGenre() {
    return genre;
  }

  public void setGenre(String genre) {
    this.genre = genre;
  }

  public String getDirector() {
    return director;
  }

  public void setDirector(String director) {
    this.director = director;
  }

  public List<String> getActors() {
    return actors;
  }

  public void setActors(List<String> actors) {
    this.actors = actors;
  }

  public String getTrailerURL() {
    return trailerURL;
  }

  public void setTrailerURL(String trailerURL) {
    this.trailerURL = trailerURL;
  }
}
