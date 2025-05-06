package ch.uzh.ifi.hase.soprafs25.rest.dto;


public class VotingStatusDTO {

    private Long userId;
    private String username;
    private boolean hasVoted;

    public VotingStatusDTO(Long userId, String username, boolean hasVoted) {
        this.userId = userId;
        this.username = username;
        this.hasVoted = hasVoted;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }
}