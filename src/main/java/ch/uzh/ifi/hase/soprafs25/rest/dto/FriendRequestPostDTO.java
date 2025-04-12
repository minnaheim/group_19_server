package ch.uzh.ifi.hase.soprafs25.rest.dto;

public class FriendRequestPostDTO {
    // here also anything in the future can be added - for now there is probably no need
    private Long receiverId;

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
} 