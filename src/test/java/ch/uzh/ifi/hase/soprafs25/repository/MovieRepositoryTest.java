package ch.uzh.ifi.hase.soprafs25.repository;

import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class MovieRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MovieRepository movieRepository;

    private Movie movie1;
    private Movie movie2;
    private Movie movie3;

    @BeforeEach
    public void setup() {
        // Create test movies
        movie1 = new Movie();
        movie1.setMovieId(1L);
        movie1.setTitle("Inception");
        movie1.setYear(2010);
        movie1.setOriginallanguage("English");
        movie1.setGenres(Arrays.asList("Sci-Fi", "Thriller", "Action"));
        movie1.setActors(Arrays.asList("Leonardo DiCaprio", "Joseph Gordon-Levitt"));
        movie1.setDirectors(Arrays.asList("Christopher Nolan"));

        movie2 = new Movie();
        movie2.setMovieId(2L);
        movie2.setTitle("The Dark Knight");
        movie2.setYear(2008);
        movie2.setOriginallanguage("English");
        movie2.setGenres(Arrays.asList("Action", "Crime", "Drama"));
        movie2.setActors(Arrays.asList("Christian Bale", "Heath Ledger"));
        movie2.setDirectors(Arrays.asList("Christopher Nolan"));

        movie3 = new Movie();
        movie3.setMovieId(3L);
        movie3.setTitle("Interstellar");
        movie3.setYear(2014);
        movie3.setOriginallanguage("English");
        movie3.setGenres(Arrays.asList("Sci-Fi", "Adventure", "Drama"));
        movie3.setActors(Arrays.asList("Matthew McConaughey", "Anne Hathaway"));
        movie3.setDirectors(Arrays.asList("Christopher Nolan"));

        // Save movies to test database
        entityManager.persist(movie1);
        entityManager.persist(movie2);
        entityManager.persist(movie3);
        entityManager.flush();
    }

    @Test
    public void testFindByMovieId() {
        // Test
        Movie foundMovie = movieRepository.findByMovieId(1L);

        // Assert
        assertNotNull(foundMovie);
        assertEquals("Inception", foundMovie.getTitle());
        assertEquals(2010, foundMovie.getYear());
    }

    @Test
    public void testFindByMovieId_NotFound() {
        // Test
        Movie foundMovie = movieRepository.findByMovieId(999L);

        // Assert
        assertNull(foundMovie);
    }

    @Test
    public void testFindByTitleContaining() {
        // Test
        List<Movie> foundMovies = movieRepository.findByTitleContaining("Dark");

        // Assert
        assertEquals(1, foundMovies.size());
        assertEquals("The Dark Knight", foundMovies.get(0).getTitle());

        // Test partial match
        foundMovies = movieRepository.findByTitleContaining("ar");
        assertEquals(2, foundMovies.size());  // Should match "Dark" and "Interstellar"
    }

    @Test
    public void testFindByYearEquals() {
        // Test
        List<Movie> foundMovies = movieRepository.findByYearEquals(2010);

        // Assert
        assertEquals(1, foundMovies.size());
        assertEquals("Inception", foundMovies.get(0).getTitle());
    }

    @Test
    public void testFindByYearEquals_NoMatch() {
        // Test
        List<Movie> foundMovies = movieRepository.findByYearEquals(2000);

        // Assert
        assertTrue(foundMovies.isEmpty());
    }

    @Test
    public void testFindByBasicSearchParams_OnlyTitle() {
        // Test
        List<Movie> foundMovies = movieRepository.findByBasicSearchParams("Inception", null);

        // Assert
        assertEquals(1, foundMovies.size());
        assertEquals("Inception", foundMovies.get(0).getTitle());
    }

    @Test
    public void testFindByBasicSearchParams_OnlyYear() {
        // Test
        List<Movie> foundMovies = movieRepository.findByBasicSearchParams(null, 2008);

        // Assert
        assertEquals(1, foundMovies.size());
        assertEquals("The Dark Knight", foundMovies.get(0).getTitle());
    }

    @Test
    public void testFindByBasicSearchParams_BothTitleAndYear() {
        // Test
        List<Movie> foundMovies = movieRepository.findByBasicSearchParams("Inter", 2014);

        // Assert
        assertEquals(1, foundMovies.size());
        assertEquals("Interstellar", foundMovies.get(0).getTitle());
    }

    @Test
    public void testFindByBasicSearchParams_NoMatches() {
        // Test
        List<Movie> foundMovies = movieRepository.findByBasicSearchParams("Nonexistent", 2000);

        // Assert
        assertTrue(foundMovies.isEmpty());
    }

    @Test
    public void testFindByBasicSearchParams_AllNullParams() {
        // Test
        List<Movie> foundMovies = movieRepository.findByBasicSearchParams(null, null);

        // Assert
        assertEquals(3, foundMovies.size());
    }

    @Test
    public void testFindByBasicSearchParams_PartialTitle() {
        // Test
        List<Movie> foundMovies = movieRepository.findByBasicSearchParams("ter", null);

        // Assert
        assertEquals(1, foundMovies.size());
        assertEquals("Interstellar", foundMovies.get(0).getTitle());
    }
}