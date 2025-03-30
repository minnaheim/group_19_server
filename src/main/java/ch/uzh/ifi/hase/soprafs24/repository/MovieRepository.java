package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import ch.uzh.ifi.hase.soprafs24.entity.Movie;

@Repository("movieRepository")
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Movie findByMovieId(long movieId);

    List<Movie> findByTitleContaining(String title);
    List<Movie> findByGenreContaining(String genre);
    List<Movie> findByActorContaining(String actor);
    List<Movie> findByCrewContaining(String crew);
    List<Movie> findByYearEquals(Integer year);

    @Query("SELECT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) AND " +
            "(:year IS NULL OR m.year = :year) AND " +
            "(:actor IS NULL OR LOWER(m.actor) LIKE LOWER(CONCAT('%', :actor, '%'))) AND " +
            "(:crew IS NULL OR LOWER(m.crew) LIKE LOWER(CONCAT('%', :crew, '%')))")
    List<Movie> findBySearchParams(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year,
            @Param("actor") String actor,
            @Param("crew") String crew);
}


