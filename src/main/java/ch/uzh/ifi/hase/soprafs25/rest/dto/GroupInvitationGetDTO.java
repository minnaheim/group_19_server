package ch.uzh.ifi.hase.soprafs25.rest.dto;
import java.time.LocalDateTime;

public class GroupInvitationGetDTO {
    
    private Long invitationId;
    private UserGetDTO sender;
    private UserGetDTO receiver;
    private GroupGetDTO group;
    private LocalDateTime creationTime;
    private Boolean accepted;
    private LocalDateTime responseTime;

    
    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public UserGetDTO getSender() {
        return sender;
    }

    public void setSender(UserGetDTO sender) {
        this.sender = sender;
    }

    public UserGetDTO getReceiver() {
        return receiver;
    }

    public void setReceiver(UserGetDTO receiver) {
        this.receiver = receiver;
    }

    public GroupGetDTO getGroup() {
        return group;
    }

    public void setGroup(GroupGetDTO group) {
        this.group = group;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(LocalDateTime responseTime) {
        this.responseTime = responseTime;
    }
} 