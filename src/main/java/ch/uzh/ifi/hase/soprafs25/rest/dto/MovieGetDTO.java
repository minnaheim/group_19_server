package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

public class MovieGetDTO {
    private long movieId;
    private String title;
    private List<String> genres;
    private Integer year;
    private List<String> actors;
    private List<String> directors;
    private List<String> spokenlanguages;
    private String originallanguage;
    private String posterURL;
    private String trailerURL;
    private String description;
    private Double tmdbRating;

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

    public String getPosterURL() {
        return posterURL;
    }

    public void setPosterURL(String posterURL) {
        this.posterURL = posterURL;
    }

    public String getTrailerURL() {
        return trailerURL;
    }

    public void setTrailerURL(String trailerURL) {
        this.trailerURL = trailerURL;
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
}