package ch.uzh.ifi.hase.soprafs25.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.ElementCollection;

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

    @Column
    private String genre;

    @Column
    private Integer year;

    @Column
    @ElementCollection
    private List<String> actor = new ArrayList<>();

    @Column
    @ElementCollection
    private List<String> director = new ArrayList<>();

    @Column
    private String originallanguage;

    @Column
    private String trailerURL;

    @Column
    private String posterURL;

    @Column
    private String description;

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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<String> getActor() {
        return actor;
    }

    public void setActor(String actor) {
        List<String> actorList = new ArrayList<>();
        actorList.add(actor);
        this.actor = actorList;
    }

    public List<String> getDirector() {
        return director;
    }

    public void setDirector(String director) {
        List<String> directorList = new ArrayList<>();
        directorList.add(director);
        this.director = directorList;
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
}