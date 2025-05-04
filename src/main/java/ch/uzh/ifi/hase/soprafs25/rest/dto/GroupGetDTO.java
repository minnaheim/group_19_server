package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GroupGetDTO {
    
    private Long groupId;
    private String groupName;
    private Long creatorId;
    private UserGetDTO creator;
    private List<Long> memberIds;
    private List<Long> movieIds;
    private String phase;
    private Integer poolPhaseDuration;
    private Integer votingPhaseDuration;
    private LocalDateTime phaseStartTime;
    private Long remainingTime;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }

    public List<Long> getMovieIds() {
        return movieIds;
    }

    public void setMovieIds(List<Long> movieIds) {
        this.movieIds = movieIds;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public UserGetDTO getCreator() {
        return creator;
    }

    public void setCreator(UserGetDTO creator) {
        this.creator = creator;
    }

    public Integer getPoolPhaseDuration() {
    return poolPhaseDuration;
}

    public void setPoolPhaseDuration(Integer poolPhaseDuration) {
        this.poolPhaseDuration = poolPhaseDuration;
    }

    public Integer getVotingPhaseDuration() {
        return votingPhaseDuration;
    }

    public void setVotingPhaseDuration(Integer votingPhaseDuration) {
        this.votingPhaseDuration = votingPhaseDuration;
    }

    public LocalDateTime getPhaseStartTime() {
        return phaseStartTime;
    }

    public void setPhaseStartTime(LocalDateTime phaseStartTime) {
        this.phaseStartTime = phaseStartTime;
    }

    public Long getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(Long remainingTime) {
        this.remainingTime = remainingTime;
    }
} 