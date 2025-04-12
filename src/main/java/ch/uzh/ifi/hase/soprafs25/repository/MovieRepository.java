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

    List<Movie> findByYearEquals(Integer year);

    @Query("SELECT DISTINCT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:year IS NULL OR m.year = :year)")
    List<Movie> findByBasicSearchParams(
            @Param("title") String title,
            @Param("year") Integer year);

    // The filtering logic will return any movie that contains at least one actor or one director from the provided lists.
    /*
    * How it works:
    * 1. It calls the basic search method findByBasicSearchParams to get initial movies
    *    filtered by title, genre, and year. If only genres are passed (e.g., `["Action", "Adventure", "Drama"]`)
    *    and both `title` and `year` are null, the `findByBasicSearchParams(title, year)` call will NOT return an
    *    empty list. Instead, it will return ALL movies from the database.
    * 2. If the provided actors or directors, genres lists are null or empty, the method returns
    *    the basic results without further filtering.
    * 3. If a list of actors/directors/genres is provided, it filters the result to only include movies where
    *    the movie's actors/directors/genres list contains at least one element from the provided actors list.
    */
    //TODO delete if not necessary
    default List<Movie> findBySearchParamsWithLists(String title, List<String> genres, Integer year,
                                                    List<String> actors, List<String> directors) {
        List<Movie> results = findByBasicSearchParams(title, year);

        if ((genres == null || genres.isEmpty()) && (actors == null || actors.isEmpty()) && (directors == null || directors.isEmpty())) {
            return results;
        }

        if (genres != null && !genres.isEmpty()) {
            results = results.stream()
                    .filter(movie -> movie.getGenres() != null &&
                            movie.getGenres().stream().anyMatch(genres::contains))
                    .collect(Collectors.toList());
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

