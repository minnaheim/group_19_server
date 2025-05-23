package ch.uzh.ifi.hase.soprafs25.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "USER_GROUP")
public class Group implements Serializable {

    public enum GroupPhase {
        POOLING,
        VOTING,
        RESULTS
    }

    @Column(nullable = false)
    private GroupPhase phase = GroupPhase.POOLING; // default phase

    
    @Id
    @GeneratedValue
    private Long groupId;

    @Column(nullable = false, unique = true)
    private String groupName;

    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members;

    // cascade for sync actions to group with moviepool
    @OneToOne(mappedBy = "group", cascade = CascadeType.ALL)
    private MoviePool moviePool;

    // timer handling
    @Column
    private Integer poolPhaseDuration;  

    @Column
    private Integer votingPhaseDuration; 

    @Column
    private LocalDateTime phaseStartTime;
    
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

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public MoviePool getMoviePool() {
        return moviePool;
    }
    public void setMoviePool(MoviePool moviePool) {
        this.moviePool = moviePool;
    }

    public GroupPhase getPhase() {
        return phase;
    }

    public void setPhase(GroupPhase phase) {
        this.phase = phase;
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
}