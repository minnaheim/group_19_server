package ch.uzh.ifi.hase.soprafs25.rest.dto;

public class DirectorDTO {
    private Long directorId;
    private String directorname;

    public Long getDirectorId() {
        return directorId;
    }

    public void setDirectorId(Long directorId) {
        this.directorId = directorId;
    }

    public String getDirectorName() {
        return directorname;
    }

    public void setDirectorName(String directorname) {
        this.directorname = directorname;
    }
}
