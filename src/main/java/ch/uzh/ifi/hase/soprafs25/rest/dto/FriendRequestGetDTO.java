package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.time.LocalDateTime;

public class FriendRequestGetDTO {
    
    private Long requestId;
    private UserGetDTO sender;
    private UserGetDTO receiver;
    private LocalDateTime creationTime;
    private Boolean accepted;
    private LocalDateTime responseTime;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
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