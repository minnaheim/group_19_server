package ch.uzh.ifi.hase.soprafs25.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "MOVIE_POOL")
public class MoviePool implements Serializable {

    @Id
    @GeneratedValue
    private Long poolId;

    @OneToOne
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private Group group;

    @ManyToMany
    @JoinTable(
        name = "movie_pool_movies",
        joinColumns = @JoinColumn(name = "pool_id"),
        inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private List<Movie> movies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "moviepool_user_movies", 
        joinColumns = @JoinColumn(name = "pool_id"))
    @MapKeyJoinColumn(name = "movie_id")
    @Column(name = "user_id")
    private Map<Movie, Long> userAddedMovies = new HashMap<>();

    @Column
    private LocalDateTime lastUpdated;

    public Long getPoolId() {
        return poolId;
    }

    public void setPoolId(Long poolId) {
        this.poolId = poolId;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Map<Movie, Long> getUserAddedMovies() {
        return userAddedMovies;
    }

    public void setUserAddedMovies(Map<Movie, Long> userAddedMovies) {
        this.userAddedMovies = userAddedMovies;
    }

    // to restrict number of suggestions to 2
    public int getMoviesAddedByUser(Long userId) {
        return (int) userAddedMovies.values().stream()
            .filter(id -> id.equals(userId))
            .count();
    }

    public void addMovie(Movie movie, Long userId) {
        this.movies.add(movie);
        this.userAddedMovies.put(movie, userId);
    }

    public void removeMovie(Movie movie) {
        this.movies.remove(movie);
        this.userAddedMovies.remove(movie);
    }

} 