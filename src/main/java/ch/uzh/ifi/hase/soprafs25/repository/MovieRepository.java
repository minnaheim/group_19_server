package ch.uzh.ifi.hase.soprafs25.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;

@Repository("movieRepository")
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Movie findByMovieId(long movieId);

    List<Movie> findByTitleContaining(String title);
    List<Movie> findByGenreContaining(String genre);
    List<Movie> findByActorContaining(String actor);
    List<Movie> findByDirectorContaining(String director);
    List<Movie> findByYearEquals(Integer year);

    @Query("SELECT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) AND " +
            "(:year IS NULL OR m.year = :year) AND " +
            "(:actor IS NULL OR EXISTS (SELECT a FROM m.actor a WHERE LOWER(a) LIKE LOWER(CONCAT('%', :actor, '%')))) AND " +
            "(:director IS NULL OR EXISTS (SELECT d FROM m.director d WHERE LOWER(d) LIKE LOWER(CONCAT('%', :director, '%'))))")
    List<Movie> findBySearchParams(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year,
            @Param("actor") String actor,
            @Param("director") String director);
}