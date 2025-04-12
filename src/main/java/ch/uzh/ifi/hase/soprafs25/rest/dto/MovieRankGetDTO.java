package ch.uzh.ifi.hase.soprafs25.rest.dto;

public class MovieRankGetDTO {
    private Long movieId;
    private String title;
    // Add other relevant movie fields if needed, e.g., posterUrl

    // Getters and Setters
    public Long getMovieId() {
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
}
