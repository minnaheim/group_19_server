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
    List<Movie> findByYearEquals(Integer year);
    List<Movie> findByLanguageContaining(String language);
    List<Movie> findByCountryContaining(String country);

    @Query("SELECT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) AND " +
            "(:year IS NULL OR m.year = :year) AND " +
            "(:country IS NULL OR LOWER(m.country) LIKE LOWER(CONCAT('%', :country, '%'))) AND " +
            "(:actor IS NULL OR LOWER(m.actor) LIKE LOWER(CONCAT('%', :actor, '%'))) AND " +
            "(:language IS NULL OR LOWER(m.language) LIKE LOWER(CONCAT('%', :language, '%'))) AND " +
            "(:trailerURL IS NULL OR LOWER(m.trailerURL) LIKE LOWER(CONCAT('%', :trailerURL, '%')))")
    List<Movie> findBySearchParams(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year,
            @Param("country") String country,
            @Param("actor") String actor,
            @Param("language") String language,
            @Param("trailerURL") String trailerURL);
}


