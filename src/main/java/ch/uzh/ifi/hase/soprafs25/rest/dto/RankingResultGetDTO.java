package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.time.LocalDateTime;

public class RankingResultGetDTO {

    private Long id;
    private MovieRankGetDTO winningMovie;
    private Double averageRank;
    private LocalDateTime calculationTimestamp;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MovieRankGetDTO getWinningMovie() {
        return winningMovie;
    }

    public void setWinningMovie(MovieRankGetDTO winningMovie) {
        this.winningMovie = winningMovie;
    }

    public Double getAverageRank() {
        return averageRank;
    }

    public void setAverageRank(Double averageRank) {
        this.averageRank = averageRank;
    }

    public LocalDateTime getCalculationTimestamp() {
        return calculationTimestamp;
    }

    public void setCalculationTimestamp(LocalDateTime calculationTimestamp) {
        this.calculationTimestamp = calculationTimestamp;
    }
}
