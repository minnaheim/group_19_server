package ch.uzh.ifi.hase.soprafs25.rest.dto;

public class PoolEntryGetDTO {
    private MovieGetDTO movie;
    private Long addedBy;

    public MovieGetDTO getMovie() {
        return movie;
    }

    public void setMovie(MovieGetDTO movie) {
        this.movie = movie;
    }

    public Long getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(Long addedBy) {
        this.addedBy = addedBy;
    }
}
