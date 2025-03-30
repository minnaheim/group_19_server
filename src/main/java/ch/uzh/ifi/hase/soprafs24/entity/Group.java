package ch.uzh.ifi.hase.soprafs24.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "GROUP")
public class Group implements Serializable {
    
    @Id
    @GeneratedValue
    private Long groupId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private User creator;

    @Column(nullable = false)
    private List<User> members;

    @Column
    private List<Movie> moviePool;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<Movie> getMoviePool() {
        return moviePool;
    }

    public void setMoviePool(List<Movie> moviePool) {
        this.moviePool = moviePool;
    }


}