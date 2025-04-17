package ch.uzh.ifi.hase.soprafs25.rest.dto;

/**
 * DTO representing a movie along with its calculated average rank within a group's ranking.
 */
public class MovieAverageRankDTO {

    private MovieGetDTO movie;
    private Double averageRank; // Can be null if no rankings exist for this movie

    // Getters and Setters
    public MovieGetDTO getMovie() {
        return movie;
    }

    public void setMovie(MovieGetDTO movie) {
        this.movie = movie;
    }

    public Double getAverageRank() {
        return averageRank;
    }

    public void setAverageRank(Double averageRank) {
        this.averageRank = averageRank;
    }
}
