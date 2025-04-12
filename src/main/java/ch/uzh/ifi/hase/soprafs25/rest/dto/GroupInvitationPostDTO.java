package ch.uzh.ifi.hase.soprafs25.rest.dto;

public class GroupInvitationPostDTO {

    private Long groupId;
    private Long receiverId;

    // here we can also have a group entity as an attribute Group (like in GetDTO), but I don't see any need for it now
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
} 