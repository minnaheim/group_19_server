package ch.uzh.ifi.hase.soprafs25.rest.dto;

// import java.util.List;

public class MoviePoolPostDTO {
    private Long movieId;

    // for optimized transformation - not sure here what approach is better: ids or movies (entities)

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }
} 