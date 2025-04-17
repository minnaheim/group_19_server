package ch.uzh.ifi.hase.soprafs25.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "RANKING_SUBMISSION_LOG")
public class RankingSubmissionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Column(nullable = false)
    @NotNull
    private LocalDateTime submissionTime;

    @Column
    private Integer numberOfMoviesRanked;
    
    // Could add more details like rankingPeriodId if needed later
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }

    public Integer getNumberOfMoviesRanked() {
        return numberOfMoviesRanked;
    }

    public void setNumberOfMoviesRanked(Integer numberOfMoviesRanked) {
        this.numberOfMoviesRanked = numberOfMoviesRanked;
    }
}
