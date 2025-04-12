package ch.uzh.ifi.hase.soprafs25.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "RANKING_RESULT")
public class RankingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional: Link to a specific ranking period if that concept is formalized
    // @Column(nullable = false)
    // private Long rankingPeriodId;

    @ManyToOne(fetch = FetchType.EAGER) // Eager fetch might be useful for displaying winner details
    @JoinColumn(name = "winning_movie_id", nullable = false)
    @NotNull
    private Movie winningMovie;

    @Column(nullable = false)
    @NotNull
    private Double averageRank; // Store the calculated average rank

    @Column(nullable = false)
    @NotNull
    private LocalDateTime calculationTimestamp;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getWinningMovie() {
        return winningMovie;
    }

    public void setWinningMovie(Movie winningMovie) {
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
