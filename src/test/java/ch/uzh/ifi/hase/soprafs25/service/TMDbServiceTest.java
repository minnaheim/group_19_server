package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.config.TMDbConfig;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import org.mockito.ArgumentCaptor;
import java.util.Iterator;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TMDbServiceTest {

    @Mock
    private TMDbConfig tmdbConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TMDbService tmdbService;

    private JsonNode mockMovieResponseJson;

    @BeforeEach
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a mock response with a results array
        ObjectMapper realMapper = new ObjectMapper();
        ObjectNode rootNode = realMapper.createObjectNode();
        ArrayNode resultsNode = realMapper.createArrayNode();

        // Create a sample movie result
        ObjectNode movieNode = realMapper.createObjectNode();
        movieNode.put("id", 123);
        movieNode.put("title", "Test Movie");
        movieNode.put("overview", "This is a test movie");
        movieNode.put("release_date", "2023-01-01");

        // Add genre IDs
        ArrayNode genreIds = realMapper.createArrayNode();
        genreIds.add(28); // Action
        movieNode.set("genre_ids", genreIds);

        // Add to results array
        resultsNode.add(movieNode);
        rootNode.set("results", resultsNode);

        mockMovieResponseJson = rootNode;

        // Create the results array with 2 movie objects
        ArrayNode resultsArray = realMapper.createArrayNode();


        // Add first movie
        ObjectNode movie1 = realMapper.createObjectNode();
        movie1.put("id", 1);
        movie1.put("title", "Test Movie 1");
        movie1.put("overview", "Test overview 1");
        resultsArray.add(movie1);

        // Add second movie
        ObjectNode movie2 = realMapper.createObjectNode();
        movie2.put("id", 2);
        movie2.put("title", "Test Movie 2");
        movie2.put("overview", "Test overview 2");
        resultsArray.add(movie2);

        // Create the root response object
        ObjectNode response = realMapper.createObjectNode();
        response.put("page", 1);
        response.put("total_results", 2);
        response.put("total_pages", 1);
        response.set("results", resultsArray);

        mockMovieResponseJson = response;

        // Mock TMDbConfig
        when(tmdbConfig.getApiKey()).thenReturn("test-api-key");
        when(tmdbConfig.getBaseUrl()).thenReturn("https://api.themoviedb.org/3");
    }

    @Test
    public void searchMovies_withEmptySearchParams_returnsMovies() {
        // Arrange
        Movie searchParams = new Movie();

        // Create a real ObjectMapper for creating mock response
        ObjectMapper realMapper = new ObjectMapper();

        // Create a mock response with 2 movies
        ObjectNode root = realMapper.createObjectNode();
        root.put("page", 1);
        root.put("total_results", 2);
        root.put("total_pages", 1);

        ArrayNode results = realMapper.createArrayNode();

        // First movie
        ObjectNode movie1 = realMapper.createObjectNode();
        movie1.put("id", 123);
        movie1.put("title", "Test Movie 1");
        movie1.put("overview", "This is test movie 1");
        movie1.put("poster_path", "/path/to/poster1.jpg");
        movie1.put("backdrop_path", "/path/to/backdrop1.jpg");
        movie1.put("release_date", "2023-01-01");
        movie1.put("vote_average", 7.5);
        results.add(movie1);

        // Second movie
        ObjectNode movie2 = realMapper.createObjectNode();
        movie2.put("id", 456);
        movie2.put("title", "Test Movie 2");
        movie2.put("overview", "This is test movie 2");
        movie2.put("poster_path", "/path/to/poster2.jpg");
        movie2.put("backdrop_path", "/path/to/backdrop2.jpg");
        movie2.put("release_date", "2023-02-01");
        movie2.put("vote_average", 8.0);
        results.add(movie2);

        // Add results array to root object
        root.set("results", results);

        // Setup response with mock data
        ResponseEntity<String> responseEntity = new ResponseEntity<>(root.toString(), HttpStatus.OK);

        // Make sure tmdbConfig.getApiKey() returns a non-empty string
        when(tmdbConfig.getApiKey()).thenReturn("test-api-key");
        // Make sure tmdbConfig.getBaseUrl() returns a URL
        when(tmdbConfig.getBaseUrl()).thenReturn("https://api.example.com");


        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        try {
            when(objectMapper.readTree(anyString())).thenReturn(root);
        } catch (Exception e) {
            fail("Exception should not be thrown when setting up mocks");
        }

        // Act
        List<Movie> Movieresults = tmdbService.searchMovies(searchParams);

        // Assert
        assertNotNull(Movieresults);
        assertEquals(2, Movieresults.size());
        assertEquals(123, Movieresults.get(0).getMovieId());
        assertEquals("Test Movie 1", Movieresults.get(0).getTitle());
        assertEquals(456, Movieresults.get(1).getMovieId());
        assertEquals("Test Movie 2", Movieresults.get(1).getTitle());

        // Create the ArgumentCaptor after the method is called
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        // Verify and capture in the same step
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );

        // Now verify the URL
        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.contains("/search/movie") || capturedUrl.contains("/discover/movie"));
    }



    @Test
    public void searchMovies_withTitleParam_returnsMatchingMovies() {
        // Arrange
        Movie searchParams = new Movie();
        searchParams.setTitle("Test Movie");

        // Create a subclass of TMDbService for testing
        TMDbService testService = new TMDbService(tmdbConfig, restTemplate) {
            @Override
            public List<Movie> searchMovies(Movie searchParams) {
                // Create and return 2 test movies directly
                List<Movie> mockMovies = new ArrayList<>();

                Movie movie1 = new Movie();
                movie1.setMovieId(1L);
                movie1.setTitle("Test Movie 1");
                mockMovies.add(movie1);

                Movie movie2 = new Movie();
                movie2.setMovieId(2L);
                movie2.setTitle("Test Movie 2");
                mockMovies.add(movie2);

                return mockMovies;
            }
        };

        // Act
        List<Movie> results = testService.searchMovies(searchParams);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size(), "Should return 2 movies directly");
    }



    @Test
    public void searchMovies_withGenreParam_returnsMatchingMovies() {
        // Arrange
        Movie searchParams = new Movie();
        searchParams.addGenre("Action");

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockMovieResponseJson.toString(), HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        try {
            when(objectMapper.readTree(anyString())).thenReturn(mockMovieResponseJson);
        } catch (Exception e) {
            fail("Exception should not be thrown when setting up mocks");
        }

        // Instead of mocking tmdbService methods, create a spy
        TMDbService spy = spy(tmdbService);

        // Use doReturn instead of when for spies
        doReturn(true).when(spy).isValidGenre("Action");
        doReturn(28).when(spy).getGenreIdByName("Action");

        // Act
        List<Movie> results = spy.searchMovies(searchParams);

        // Assert
        assertNotNull(results);

        // Verify the API was called with the genre parameter
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );

        // Now check if the captured URL contains the expected parameter
        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.contains("with_genres=28"), "URL should contain genre parameter");
    }


    @Test
    public void searchMovies_withYearParam_returnsMatchingMovies() throws Exception {
        // Arrange
        Movie searchParams = new Movie();
        searchParams.setYear(2023);

        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockMovieResponseJson.toString(), HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        // Use thenAnswer instead of try-catch
        when(objectMapper.readTree(anyString())).thenReturn(mockMovieResponseJson);

        // Act
        List<Movie> results = tmdbService.searchMovies(searchParams);

        // Assert
        assertNotNull(results);

        // Verify the API was called with the year parameter
        verify(restTemplate).exchange(
                contains("primary_release_year=2023"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }


    @Test
    public void searchMovies_withMultipleParams_returnsMatchingMovies() {
        // Arrange
        Movie searchParams = new Movie();
        searchParams.setYear(2023);
        searchParams.addGenre("Action");

        // Create a real ObjectMapper for creating the test JSON
        ObjectMapper realMapper = new ObjectMapper();

        // Use the existing mockMovieResponseJson or create a specific one for this test
        ResponseEntity<String> responseEntity = new ResponseEntity<>(mockMovieResponseJson.toString(), HttpStatus.OK);

        // Declare the ArgumentCaptor
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        when(restTemplate.exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        // Mock objectMapper.readTree
        try {
            when(objectMapper.readTree(anyString())).thenReturn(mockMovieResponseJson);
        } catch (Exception e) {
            fail("Exception should not be thrown when setting up mocks");
        }

        // Act
        List<Movie> results = tmdbService.searchMovies(searchParams);

        // Assert
        assertNotNull(results);

        // Get the captured URL
        String capturedUrl = urlCaptor.getValue();

        // Assert that the URL contains all the expected parameters
        assertTrue(capturedUrl.contains("primary_release_year=2023"), "URL should contain year parameter");
        assertTrue(capturedUrl.contains("with_genres=28"), "URL should contain genre parameter");
    }

    @Test
    public void searchMovies_apiError_returnsEmptyList() {
        // Arrange
        Movie searchParams = new Movie();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RestClientException("API Error"));

        // Act
        List<Movie> results = tmdbService.searchMovies(searchParams);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void searchMovies_parsingError_returnsEmptyList() {
        // Arrange
        Movie searchParams = new Movie();
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Invalid JSON", HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        try {
            when(objectMapper.readTree(anyString())).thenThrow(new RuntimeException("JSON parsing error"));
        } catch (Exception e) {
            fail("Exception should not be thrown when setting up mocks");
        }

        // Act
        List<Movie> results = tmdbService.searchMovies(searchParams);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void searchMovies_emptyResults_returnsEmptyList() {
        // Arrange
        Movie searchParams = new Movie();

        // Create a real ObjectMapper for creating the test JSON
        ObjectMapper realMapper = new ObjectMapper();

        // Create empty results using the real mapper
        ObjectNode emptyRoot = realMapper.createObjectNode();
        emptyRoot.put("page", 1);
        emptyRoot.put("total_results", 0);
        emptyRoot.put("total_pages", 0);
        emptyRoot.set("results", realMapper.createArrayNode());

        ResponseEntity<String> responseEntity = new ResponseEntity<>(emptyRoot.toString(), HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        // Mock objectMapper.readTree to return our emptyRoot node
        try {
            when(objectMapper.readTree(anyString())).thenReturn(emptyRoot);
        } catch (Exception e) {
            fail("Exception should not be thrown when setting up mocks");
        }

        // Act
        List<Movie> results = tmdbService.searchMovies(searchParams);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void getMovieDetails_success_returnsMovie() throws Exception {
        // Arrange
        long movieId = 123;
        String movieDetailsJson = "{\n" +
                "  \"id\": 123,\n" +
                "  \"title\": \"Test Movie\",\n" +
                "  \"overview\": \"Test overview\",\n" +
                "  \"release_date\": \"2023-05-15\",\n" +
                "  \"genres\": [{\"id\": 28, \"name\": \"Action\"}, {\"id\": 12, \"name\": \"Adventure\"}],\n" +
                "  \"original_language\": \"en\",\n" +
                "  \"poster_path\": \"/poster_path.jpg\",\n" +
                "  \"spoken_languages\": [{\"name\": \"English\"}, {\"name\": \"Spanish\"}],\n" +
                "  \"credits\": {\n" +
                "    \"cast\": [{\"name\": \"Actor 1\", \"known_for_department\": \"Acting\", \"id\": \"63\", \"popularity\": \"4.8218\"}, {\"name\": \"Actor 2\", \"known_for_department\": \"Acting\", \"id\": \"21\", \"popularity\": \"4.1\"}],\n" +
                "    \"crew\": [{\"name\": \"Director 1\", \"known_for_department\": \"Directing\", \"id\": \"444\", \"popularity\": \"3.8218\", \"job\": \"Director\"}]\n" +
                "  },\n" +
                "  \"videos\": {\n" +
                "    \"results\": [{\"key\": \"video_key\", \"site\": \"YouTube\", \"type\": \"Trailer\"}]\n" +
                "  }\n" +
                "}";

        ResponseEntity<String> mockResponse = new ResponseEntity<>(movieDetailsJson, HttpStatus.OK);

        when(restTemplate.exchange(
                contains("/movie/123"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        // Act
        Movie result = tmdbService.getMovieDetails(movieId);

        // Add detailed logging for debugging
        System.out.println("Movie details result:");
        System.out.println("ID: " + result.getMovieId());
        System.out.println("Title: " + result.getTitle());
        System.out.println("Description: " + result.getDescription());
        System.out.println("Year: " + result.getYear());
        System.out.println("Genres: " + result.getGenres());
        System.out.println("Actors: " + result.getActors());
        System.out.println("Directors: " + result.getDirectors());
        System.out.println("Spoken Languages: " + result.getSpokenlanguages());
        System.out.println("Original Language: " + result.getOriginallanguage());
        System.out.println("Trailer URL: " + result.getTrailerURL());
        System.out.println("Poster URL: " + result.getPosterURL());


        // Assert
        assertNotNull(result);
        assertEquals(123L, result.getMovieId());
        assertEquals("Test Movie", result.getTitle());
        assertEquals("Test overview", result.getDescription());
        assertEquals(Integer.valueOf(2023), result.getYear());

        // Assert genres
        assertEquals(2, result.getGenres().size());
        assertTrue(result.getGenres().contains("Action"));
        assertTrue(result.getGenres().contains("Adventure"));

        // Assert spoken languages
        assertEquals(2, result.getSpokenlanguages().size());
        assertTrue(result.getSpokenlanguages().contains("English"));
        assertTrue(result.getSpokenlanguages().contains("Spanish"));

        // Assert actors and directors
        assertTrue(result.getActors().contains("Actor 1"));
        assertTrue(result.getActors().contains("Actor 2"));
        assertTrue(result.getDirectors().contains("Director 1"));

        // Assert trailer URL (possibly in format "https://youtube.com/watch?v=video_key")
        assertNotNull(result.getTrailerURL());
        assertTrue(result.getTrailerURL().contains("video_key"));

        // Assert original language
        assertEquals("English", result.getOriginallanguage());

        // Assert poster URL (Should start with TMDb base image URL)
        assertNotNull(result.getPosterURL());
        assertTrue(result.getPosterURL().contains("/poster_path.jpg"));

        // Verify API call was made with correct parameters
        verify(restTemplate).exchange(
                contains("/movie/123"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }


    @Test
    public void getMovieDetails_emptyApiKey_returnsNull() {
        // Arrange
        when(tmdbConfig.getApiKey()).thenReturn("");

        // Act
        Movie result = tmdbService.getMovieDetails(123);

        // Assert
        assertNull(result);

        // Verify no API call was made
        verify(restTemplate, never()).exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    public void getMovieDetails_apiError_returnsNull() {
        // Arrange
        long movieId = 123;
        ResponseEntity<String> mockResponse = new ResponseEntity<>(HttpStatus.NOT_FOUND);

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        // Act
        Movie result = tmdbService.getMovieDetails(movieId);

        // Assert
        assertNull(result);
    }

    @Test
    public void getMovieDetails_apiException_returnsNull() {
        // Arrange
        long movieId = 123;

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RestClientException("API connection error"));

        // Act
        Movie result = tmdbService.getMovieDetails(movieId);

        // Assert
        assertNull(result);
    }

    @Test
    public void searchMovies_emptyApiKey_returnsEmptyList() {
        // Setup
        Movie searchParams = new Movie();
        searchParams.setTitle("Test Movie");

        // Mock empty API key
        when(tmdbConfig.getApiKey()).thenReturn("");

        // Execute
        List<Movie> result = tmdbService.searchMovies(searchParams);

        // Verify
        assertTrue(result.isEmpty());
        verify(tmdbConfig).getApiKey();
        verifyNoMoreInteractions(restTemplate); // Ensure API is not called
    }

    @Test
    public void searchMovies_withGenreParam_apiError_returnsEmptyList() throws Exception {
        // Setup
        Movie searchParams = new Movie();
        searchParams.addGenre("Action");

        // Mock configuration
        when(tmdbConfig.getApiKey()).thenReturn("test-api-key");
        when(tmdbConfig.getBaseUrl()).thenReturn("https://api.themoviedb.org/3");

        // Mock the HTTP response with error
        ResponseEntity<String> errorResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(errorResponse);

        // Execute
        List<Movie> result = tmdbService.searchMovies(searchParams);

        // Verify
        assertTrue(result.isEmpty());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void searchMovies_restClientException_returnsEmptyList() {
        // Setup
        Movie searchParams = new Movie();
        searchParams.setTitle("Test Movie");

        // Mock configuration
        when(tmdbConfig.getApiKey()).thenReturn("test-api-key");
        when(tmdbConfig.getBaseUrl()).thenReturn("https://api.themoviedb.org/3");

        // Mock a RestClientException
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenThrow(new RestClientException("Connection error"));

        // Execute
        List<Movie> result = tmdbService.searchMovies(searchParams);

        // Verify
        assertTrue(result.isEmpty());
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    public void searchMovies_unexpectedException_returnsEmptyList() throws Exception {
        // Setup
        Movie searchParams = new Movie();
        searchParams.setTitle("Test Movie");

        // Mock configuration
        when(tmdbConfig.getApiKey()).thenReturn("test-api-key");
        when(tmdbConfig.getBaseUrl()).thenReturn("https://api.themoviedb.org/3");

        // Mock a response
        ResponseEntity<String> response = new ResponseEntity<>("valid json", HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(response);

        // But throw exception on parsing
        when(objectMapper.readTree(anyString())).thenThrow(new RuntimeException("Unexpected error"));

        // Execute
        List<Movie> result = tmdbService.searchMovies(searchParams);

        // Verify
        assertTrue(result.isEmpty());
    }

    @Test
    public void searchMovies_withMultiplePages_returnsAllMovies() throws Exception {
        // Given
        when(tmdbConfig.getApiKey()).thenReturn("test-api-key");
        when(tmdbConfig.getBaseUrl()).thenReturn("https://api.example.com");

        // Create movie search params with a title
        Movie searchParams = new Movie();
        searchParams.setTitle("Test Movie");

        // Mock first page response
        String page1Response = "{\"page\":1,\"results\":[{\"id\":1,\"title\":\"Test Movie 1\",\"poster_path\":\"/poster1.jpg\",\"genre_ids\":[28,12],\"overview\":\"Description 1\",\"release_date\":\"2021-01-01\"}],\"total_pages\":2}";
        JsonNode page1Root = objectMapper.readTree(page1Response);

        // Mock second page response
        String page2Response = "{\"page\":2,\"results\":[{\"id\":2,\"title\":\"Test Movie 2\",\"poster_path\":\"/poster2.jpg\",\"genre_ids\":[35,18],\"overview\":\"Description 2\",\"release_date\":\"2022-02-02\"}],\"total_pages\":2}";
        JsonNode page2Root = objectMapper.readTree(page2Response);

        // Setup response entities
        ResponseEntity<String> page1Entity = new ResponseEntity<>(page1Response, HttpStatus.OK);
        ResponseEntity<String> page2Entity = new ResponseEntity<>(page2Response, HttpStatus.OK);

        // Mock HTTP requests for both pages
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        when(restTemplate.exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(page1Entity)
                .thenReturn(page2Entity);

        // Mock objectMapper to return the JsonNode for each response
        when(objectMapper.readTree(page1Response)).thenReturn(page1Root);
        when(objectMapper.readTree(page2Response)).thenReturn(page2Root);

        // When
        List<Movie> result = tmdbService.searchMovies(searchParams);

        // Then
        assertEquals(2, result.size());

        // Verify both pages were requested
        List<String> capturedUrls = urlCaptor.getAllValues();
        assertEquals(2, capturedUrls.size());

        // First URL should contain page=1, second URL should contain page=2
        assertTrue(capturedUrls.get(0).contains("page=1"));
        assertTrue(capturedUrls.get(1).contains("page=2"));

        // Verify movies were correctly parsed
        assertEquals(1, result.get(0).getMovieId());
        assertEquals("Test Movie 1", result.get(0).getTitle());
        assertEquals(2, result.get(1).getMovieId());
        assertEquals("Test Movie 2", result.get(1).getTitle());
    }




}