package ch.uzh.ifi.hase.soprafs25.rest.dto;

public class MovieGetDTO {
    private long movieId;
    private String title;
    private String genre;
    private Integer year;
    private String[] actors; // Array of actor names
    private String director;
    private String originallanguage;
    private String posterURL;
    private String trailerURL;
    private String description;

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

    public String[] getActors() {
        return actors;
    }

    public void setActors(String[] actors) {
        this.actors = actors;
    }


    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
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
}


