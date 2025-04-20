package ch.uzh.ifi.hase.soprafs25.entity;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovieTest {

    @Test
    void testMovieEntityWithAllProperties() {
        // Create a new movie instance
        Movie movie = new Movie();

        // Set all properties
        movie.setMovieId(27205);
        movie.setTitle("Inception");

        List<String> genres = Arrays.asList("Action", "ScienceFiction", "Adventure");
        movie.setGenres(genres);

        movie.setYear(2010);

        List<String> actors = Arrays.asList("LeonardoDiCaprio", "JosephGordon-Levitt",
                "KenWatanabe", "TomHardy", "ElliotPage");
        movie.setActors(actors);

        List<String> directors = Arrays.asList("ChristopherNolan", "EmmaThomas");
        movie.setDirectors(directors);

        movie.setOriginallanguage("English");
        movie.setTrailerURL("https://www.youtube.com/watch?v=mpj9dL7swwk");
        movie.setPosterURL("https://image.tmdb.org/t/p/w500/ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg");

        String description = "Cobb,askilledthiefwhocommitscorporateespionagebyinfiltratingthesubconsciousofhis" +
                "targetsisofferedachancetoregainhisoldlifeaspaymentforataskconsideredtobeimpossible:" +
                "\"inception\",theimplantationofanotherperson'sideaintoatarget'ssubconscious.";
        movie.setDescription(description);

        List<String> spokenLanguages = Arrays.asList("English", "Deutsch");
        movie.setSpokenlanguages(spokenLanguages);

        // Verify all properties
        assertEquals(27205, movie.getMovieId());
        assertEquals("Inception", movie.getTitle());
        assertEquals(genres, movie.getGenres());
        assertEquals(Integer.valueOf(2010), movie.getYear());
        assertEquals(actors, movie.getActors());
        assertEquals(directors, movie.getDirectors());
        assertEquals("English", movie.getOriginallanguage());
        assertEquals("https://www.youtube.com/watch?v=mpj9dL7swwk", movie.getTrailerURL());
        assertEquals("https://image.tmdb.org/t/p/w500/ljsZTbVsrQSqZgWeep2B1QiDKuh.jpg", movie.getPosterURL());
        assertEquals(description, movie.getDescription());
        assertEquals(spokenLanguages, movie.getSpokenlanguages());
    }

    @Test
    void testAddMethods() {
        // Create a new movie instance
        Movie movie = new Movie();

        // Test the add methods
        movie.addGenre("Action");
        movie.addGenre("ScienceFiction");
        assertEquals(2, movie.getGenres().size());
        assertTrue(movie.getGenres().contains("Action"));
        assertTrue(movie.getGenres().contains("ScienceFiction"));

        movie.addActor("LeonardoDiCaprio");
        movie.addActor("TomHardy");
        assertEquals(2, movie.getActors().size());
        assertTrue(movie.getActors().contains("LeonardoDiCaprio"));
        assertTrue(movie.getActors().contains("TomHardy"));

        movie.addDirector("ChristopherNolan");
        assertEquals(1, movie.getDirectors().size());
        assertTrue(movie.getDirectors().contains("ChristopherNolan"));

        movie.addSpokenlanguage("English");
        movie.addSpokenlanguage("Deutsch");
        assertEquals(2, movie.getSpokenlanguages().size());
        assertTrue(movie.getSpokenlanguages().contains("English"));
        assertTrue(movie.getSpokenlanguages().contains("Deutsch"));
    }

    @Test
    void testEmptyLists() {
        // Create a new movie instance
        Movie movie = new Movie();

        // Lists should be initialized as empty ArrayList instances
        assertNotNull(movie.getGenres());
        assertNotNull(movie.getActors());
        assertNotNull(movie.getDirectors());
        assertNotNull(movie.getSpokenlanguages());

        assertTrue(movie.getGenres().isEmpty());
        assertTrue(movie.getActors().isEmpty());
        assertTrue(movie.getDirectors().isEmpty());
        assertTrue(movie.getSpokenlanguages().isEmpty());
    }
}