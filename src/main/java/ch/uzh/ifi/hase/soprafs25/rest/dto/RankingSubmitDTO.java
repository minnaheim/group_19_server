package ch.uzh.ifi.hase.soprafs25.rest.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class RankingSubmitDTO {

    @NotNull(message = "Movie ID cannot be null")
    private Long movieId; // Use Long to allow null check by @NotNull

    @NotNull(message = "Rank cannot be null")
    @Min(value = 1, message = "Rank must be at least 1")
    private Integer rank; // Use Integer for null check

    // Getters and Setters
    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
