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

    List<Movie> findByActorsContaining(List<String> actors);

    List<Movie> findByDirectorsContaining(List<String> directors);

    List<Movie> findByYearEquals(Integer year);

    @Query("SELECT DISTINCT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) AND " +
            "(:year IS NULL OR m.year = :year) AND " +
            "(:actors IS NULL OR :actors IS EMPTY OR m.actors IN :actors) AND " +
            "(:directors IS NULL OR :directors IS EMPTY OR m.directors IN :directors)")
    List<Movie> findBySearchParamsWithLists(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year,
            @Param("actors") List<String> actors,
            @Param("directors") List<String> directors);
}