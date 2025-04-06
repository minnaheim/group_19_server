package ch.uzh.ifi.hase.soprafs25.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;

@Repository("movieRepository")
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Movie findByMovieId(long movieId);

    List<Movie> findByTitleContaining(String title);

    List<Movie> findByGenreContaining(String genre);

    List<Movie> findByYearEquals(Integer year);

    @Query("SELECT DISTINCT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:genre IS NULL OR LOWER(m.genre) LIKE LOWER(CONCAT('%', :genre, '%'))) AND " +
            "(:year IS NULL OR m.year = :year)")
    List<Movie> findByBasicSearchParams(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("year") Integer year);

    // The filtering logic will return any movie that contains at least one actor or one director from the provided lists.
    /*
    * How it works:
    * 1. It calls the basic search method findByBasicSearchParams to get initial movies
    *    filtered by title, genre, and year.
    * 2. If the provided actors or directors lists are null or empty, the method returns
    *    the basic results without further filtering.
    * 3. If a list of actors is provided, it filters the result to only include movies where
    *    the movie's actors list contains at least one element from the provided actors list.
    * 4. Similarly, if a list of directors is provided, it filters the result to only include movies where
    *    the movie's directors list contains at least one element from the provided directors list.
    *
    * In answer to your question: Yes, you can search via multiple actors or multiple directors.
    * The filtering logic uses anyMatch, which will return any movie that contains at least one
    * actor or director from the provided lists.
    */

    default List<Movie> findBySearchParamsWithLists(String title, String genre, Integer year,
                                                    List<String> actors, List<String> directors) {
        List<Movie> results = findByBasicSearchParams(title, genre, year);

        if ((actors == null || actors.isEmpty()) && (directors == null || directors.isEmpty())) {
            return results;
        }

        if (actors != null && !actors.isEmpty()) {
            results = results.stream()
                    .filter(movie -> movie.getActors() != null &&
                            movie.getActors().stream().anyMatch(actors::contains))
                    .collect(Collectors.toList());
        }

        if (directors != null && !directors.isEmpty()) {
            results = results.stream()
                    .filter(movie -> movie.getDirectors() != null &&
                            movie.getDirectors().stream().anyMatch(directors::contains))
                    .collect(Collectors.toList());
        }

        return results;
    }
}

