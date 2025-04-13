package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MoviePoolGetDTO {
    
    private Long poolId;
    private Long groupId;
    private List<MovieGetDTO> movies;
    private LocalDateTime lastUpdated;

    public Long getPoolId() {
        return poolId;
    }

    public void setPoolId(Long poolId) {
        this.poolId = poolId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public List<MovieGetDTO> getMovies() {
        return movies;
    }

    public void setMovies(List<MovieGetDTO> movies) {
        this.movies = movies;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 