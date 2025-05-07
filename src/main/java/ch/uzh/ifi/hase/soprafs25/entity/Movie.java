package ch.uzh.ifi.hase.soprafs25.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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

    @Id
    private long movieId; // corresponds to TMDB API movie_id

    @Column
    private String title;

    @ElementCollection
    @CollectionTable(
            name = "MOVIE_GENRES",
            joinColumns = @JoinColumn(name = "movie_id")
    )
    @Column(name = "genre")
    private List<String> genres = new ArrayList<>();

    @Column
    private Integer year;

    @ElementCollection
    @CollectionTable(
            name = "MOVIE_ACTORS",
            joinColumns = @JoinColumn(name = "movie_id")
    )
    @Column(name = "actor")
    private List<String> actors = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "MOVIE_DIRECTORS",
            joinColumns = @JoinColumn(name = "movie_id")
    )
    @Column(name = "director")
    private List<String> directors = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "MOVIE_SPOKENLANGUAGES",
            joinColumns = @JoinColumn(name = "movie_id")
    )
    @Column(name = "spokenlanguages")
    private List<String> spokenlanguages = new ArrayList<>();

    @Column
    private String originallanguage;

    @Column
    private String trailerURL;

    @Column
    private String posterURL;

    @Column(length = 10000) //inorder to get large descriptions
    private String description;

    // for tie-breaks in ranking
    @Column
    private Double tmdbRating;
    
    // Getters and setters
    public long getMovieId() {
        return movieId;
    }

    public void setMovieId(long movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public void addGenre(String genre) {
        this.genres.add(genre);
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public void addActor(String actor) {
        this.actors.add(actor);
    }

    public List<String> getDirectors() {
        return directors;
    }

    public void setDirectors(List<String> directors) {
        this.directors = directors;
    }

    public void addDirector(String director) {
        this.directors.add(director);
    }

    public List<String> getSpokenlanguages() {
        return spokenlanguages;
    }

    public void setSpokenlanguages(List<String> spokenlanguages) {
        this.spokenlanguages = spokenlanguages;
    }

    public void addSpokenlanguage(String spokenlanguage) {
        this.spokenlanguages.add(spokenlanguage);
    }

    public String getOriginallanguage() {
        return originallanguage;
    }

    public void setOriginallanguage(String originallanguage) {
        this.originallanguage = originallanguage;
    }

    public String getTrailerURL() {
        return trailerURL;
    }

    public void setTrailerURL(String trailerURL) {
        this.trailerURL = trailerURL;
    }

    public String getPosterURL() {
        return posterURL;
    }

    public void setPosterURL(String posterURL) {
        this.posterURL = posterURL;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getTmdbRating() {
        return tmdbRating;
    }

    public void setTmdbRating(Double tmdbRating) {
        this.tmdbRating = tmdbRating;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return movieId == movie.movieId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(movieId);
    }

}