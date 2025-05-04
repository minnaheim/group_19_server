package ch.uzh.ifi.hase.soprafs25.rest.dto;

public class ActorDTO {
    private Long actorId;
    private String actorname;

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorname;
    }

    public void setActorName(String actorname) {
        this.actorname = actorname;
    }
}
