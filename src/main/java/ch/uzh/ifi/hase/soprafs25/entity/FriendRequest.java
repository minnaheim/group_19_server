package ch.uzh.ifi.hase.soprafs25.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "FRIEND_REQUEST")
public class FriendRequest {

    @Id
    @GeneratedValue
    private Long requestId;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false)
    private LocalDateTime creationTime;

    @Column
    private LocalDateTime respondTime;

    @Column(nullable = false)
    // by default set it to false after sending
    private boolean accepted = false;

    // set creation time when FriendRequest is initialized
    public FriendRequest(){
        this.creationTime = LocalDateTime.now();
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getRespondTime() {
        return respondTime;
    }

    public void setRespondTime(LocalDateTime respondTime) {
        this.respondTime = respondTime;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
        this.respondTime = LocalDateTime.now();
    }
    
    
}
