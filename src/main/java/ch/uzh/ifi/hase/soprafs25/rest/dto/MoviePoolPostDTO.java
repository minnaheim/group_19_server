package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

public class MoviePoolPostDTO {
    private List<Long> movieIds;

    // for optimized transformation - not sure here what approach is better: ids or movies (entities)

    public List<Long> getMovieIds() {
        return movieIds;
    }

    public void setMovieIds(List<Long> movieIds) {
        this.movieIds = movieIds;
    }
} 