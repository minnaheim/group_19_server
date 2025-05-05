package ch.uzh.ifi.hase.soprafs25.service;

import ch.uzh.ifi.hase.soprafs25.config.TMDbConfig;
import ch.uzh.ifi.hase.soprafs25.rest.dto.ActorDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.DirectorDTO;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.HashSet;
import java.util.Set;


@Service
public class TMDbService {

    private final Logger log = LoggerFactory.getLogger(TMDbService.class);
    private final TMDbConfig tmdbConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Map to convert genre IDs to strings
    private static final Map<Integer, String> GENRE_ID_TO_NAME = new HashMap<>();
    static {
        GENRE_ID_TO_NAME.put(28, "Action");
        GENRE_ID_TO_NAME.put(12, "Adventure");
        GENRE_ID_TO_NAME.put(16, "Animation");
        GENRE_ID_TO_NAME.put(35, "Comedy");
        GENRE_ID_TO_NAME.put(80, "Crime");
        GENRE_ID_TO_NAME.put(99, "Documentary");
        GENRE_ID_TO_NAME.put(18, "Drama");
        GENRE_ID_TO_NAME.put(10751, "Family");
        GENRE_ID_TO_NAME.put(14, "Fantasy");
        GENRE_ID_TO_NAME.put(36, "History");
        GENRE_ID_TO_NAME.put(27, "Horror");
        GENRE_ID_TO_NAME.put(10402, "Music");
        GENRE_ID_TO_NAME.put(9648, "Mystery");
        GENRE_ID_TO_NAME.put(10749, "Romance");
        GENRE_ID_TO_NAME.put(878, "Science Fiction");
        GENRE_ID_TO_NAME.put(10770, "TV Movie");
        GENRE_ID_TO_NAME.put(53, "Thriller");
        GENRE_ID_TO_NAME.put(10752, "War");
        GENRE_ID_TO_NAME.put(37, "Western");
    }

    // Map to convert genre names to IDs
    private static final Map<String, String> GENRE_NAME_TO_ID = new HashMap<>();
    static {
        GENRE_NAME_TO_ID.put("Action", "28");
        GENRE_NAME_TO_ID.put("Adventure", "12");
        GENRE_NAME_TO_ID.put("Animation", "16");
        GENRE_NAME_TO_ID.put("Comedy", "35");
        GENRE_NAME_TO_ID.put("Crime", "80");
        GENRE_NAME_TO_ID.put("Documentary", "99");
        GENRE_NAME_TO_ID.put("Drama", "18");
        GENRE_NAME_TO_ID.put("Family", "10751");
        GENRE_NAME_TO_ID.put("Fantasy", "14");
        GENRE_NAME_TO_ID.put("History", "36");
        GENRE_NAME_TO_ID.put("Horror", "27");
        GENRE_NAME_TO_ID.put("Music", "10402");
        GENRE_NAME_TO_ID.put("Mystery", "9648");
        GENRE_NAME_TO_ID.put("Romance", "10749");
        GENRE_NAME_TO_ID.put("Science Fiction", "878");
        GENRE_NAME_TO_ID.put("TV Movie", "10770");
        GENRE_NAME_TO_ID.put("Thriller", "53");
        GENRE_NAME_TO_ID.put("War", "10752");
        GENRE_NAME_TO_ID.put("Western", "37");
    }

    // Map to convert language IDs to strings
    private static final Map<String, String> LanguageID_TO_LanguageNAME = new HashMap<>();
    static {
        LanguageID_TO_LanguageNAME.put("xx", "No Language");
        LanguageID_TO_LanguageNAME.put("aa", "Afar");
        LanguageID_TO_LanguageNAME.put("af", "Afrikaans");
        LanguageID_TO_LanguageNAME.put("ak", "Akan");
        LanguageID_TO_LanguageNAME.put("an", "Aragonese");
        LanguageID_TO_LanguageNAME.put("as", "Assamese");
        LanguageID_TO_LanguageNAME.put("av", "Avaric");
        LanguageID_TO_LanguageNAME.put("ae", "Avestan");
        LanguageID_TO_LanguageNAME.put("ay", "Aymara");
        LanguageID_TO_LanguageNAME.put("az", "Azerbaijani");
        LanguageID_TO_LanguageNAME.put("ba", "Bashkir");
        LanguageID_TO_LanguageNAME.put("bm", "Bambara");
        LanguageID_TO_LanguageNAME.put("bn", "Bengali");
        LanguageID_TO_LanguageNAME.put("bi", "Bislama");
        LanguageID_TO_LanguageNAME.put("bo", "Tibetan");
        LanguageID_TO_LanguageNAME.put("bs", "Bosnian");
        LanguageID_TO_LanguageNAME.put("br", "Breton");
        LanguageID_TO_LanguageNAME.put("ca", "Catalan");
        LanguageID_TO_LanguageNAME.put("cs", "Czech");
        LanguageID_TO_LanguageNAME.put("ch", "Chamorro");
        LanguageID_TO_LanguageNAME.put("ce", "Chechen");
        LanguageID_TO_LanguageNAME.put("cu", "Slavic");
        LanguageID_TO_LanguageNAME.put("cv", "Chuvash");
        LanguageID_TO_LanguageNAME.put("kw", "Cornish");
        LanguageID_TO_LanguageNAME.put("co", "Corsican");
        LanguageID_TO_LanguageNAME.put("cr", "Cree");
        LanguageID_TO_LanguageNAME.put("cy", "Welsh");
        LanguageID_TO_LanguageNAME.put("da", "Danish");
        LanguageID_TO_LanguageNAME.put("de", "German");
        LanguageID_TO_LanguageNAME.put("dv", "Divehi");
        LanguageID_TO_LanguageNAME.put("dz", "Dzongkha");
        LanguageID_TO_LanguageNAME.put("en", "English");
        LanguageID_TO_LanguageNAME.put("eo", "Esperanto");
        LanguageID_TO_LanguageNAME.put("et", "Estonian");
        LanguageID_TO_LanguageNAME.put("eu", "Basque");
        LanguageID_TO_LanguageNAME.put("fo", "Faroese");
        LanguageID_TO_LanguageNAME.put("fj", "Fijian");
        LanguageID_TO_LanguageNAME.put("fi", "Finnish");
        LanguageID_TO_LanguageNAME.put("fr", "French");
        LanguageID_TO_LanguageNAME.put("fy", "Frisian");
        LanguageID_TO_LanguageNAME.put("ff", "Fulah");
        LanguageID_TO_LanguageNAME.put("gd", "Gaelic");
        LanguageID_TO_LanguageNAME.put("ga", "Irish");
        LanguageID_TO_LanguageNAME.put("gl", "Galician");
        LanguageID_TO_LanguageNAME.put("gv", "Manx");
        LanguageID_TO_LanguageNAME.put("gn", "Guarani");
        LanguageID_TO_LanguageNAME.put("gu", "Gujarati");
        LanguageID_TO_LanguageNAME.put("ht", "Haitian; Haitian Creole");
        LanguageID_TO_LanguageNAME.put("ha", "Hausa");
        LanguageID_TO_LanguageNAME.put("sh", "Serbo-Croatian");
        LanguageID_TO_LanguageNAME.put("hz", "Herero");
        LanguageID_TO_LanguageNAME.put("ho", "Hiri Motu");
        LanguageID_TO_LanguageNAME.put("hr", "Croatian");
        LanguageID_TO_LanguageNAME.put("hu", "Hungarian");
        LanguageID_TO_LanguageNAME.put("ig", "Igbo");
        LanguageID_TO_LanguageNAME.put("io", "Ido");
        LanguageID_TO_LanguageNAME.put("ii", "Yi");
        LanguageID_TO_LanguageNAME.put("iu", "Inuktitut");
        LanguageID_TO_LanguageNAME.put("ie", "Interlingue");
        LanguageID_TO_LanguageNAME.put("ia", "Interlingua");
        LanguageID_TO_LanguageNAME.put("id", "Indonesian");
        LanguageID_TO_LanguageNAME.put("ik", "Inupiaq");
        LanguageID_TO_LanguageNAME.put("is", "Icelandic");
        LanguageID_TO_LanguageNAME.put("it", "Italian");
        LanguageID_TO_LanguageNAME.put("jv", "Javanese");
        LanguageID_TO_LanguageNAME.put("ja", "Japanese");
        LanguageID_TO_LanguageNAME.put("kl", "Kalaallisut");
        LanguageID_TO_LanguageNAME.put("kn", "Kannada");
        LanguageID_TO_LanguageNAME.put("ks", "Kashmiri");
        LanguageID_TO_LanguageNAME.put("ka", "Georgian");
        LanguageID_TO_LanguageNAME.put("kr", "Kanuri");
        LanguageID_TO_LanguageNAME.put("kk", "Kazakh");
        LanguageID_TO_LanguageNAME.put("km", "Khmer");
        LanguageID_TO_LanguageNAME.put("ki", "Kikuyu");
        LanguageID_TO_LanguageNAME.put("rw", "Kinyarwanda");
        LanguageID_TO_LanguageNAME.put("ky", "Kirghiz");
        LanguageID_TO_LanguageNAME.put("kv", "Komi");
        LanguageID_TO_LanguageNAME.put("kg", "Kongo");
        LanguageID_TO_LanguageNAME.put("ko", "Korean");
        LanguageID_TO_LanguageNAME.put("kj", "Kuanyama");
        LanguageID_TO_LanguageNAME.put("ku", "Kurdish");
        LanguageID_TO_LanguageNAME.put("lo", "Lao");
        LanguageID_TO_LanguageNAME.put("la", "Latin");
        LanguageID_TO_LanguageNAME.put("lv", "Latvian");
        LanguageID_TO_LanguageNAME.put("li", "Limburgish");
        LanguageID_TO_LanguageNAME.put("ln", "Lingala");
        LanguageID_TO_LanguageNAME.put("lt", "Lithuanian");
        LanguageID_TO_LanguageNAME.put("lb", "Letzeburgesch");
        LanguageID_TO_LanguageNAME.put("lu", "Luba-Katanga");
        LanguageID_TO_LanguageNAME.put("lg", "Ganda");
        LanguageID_TO_LanguageNAME.put("mh", "Marshall");
        LanguageID_TO_LanguageNAME.put("ml", "Malayalam");
        LanguageID_TO_LanguageNAME.put("mr", "Marathi");
        LanguageID_TO_LanguageNAME.put("mg", "Malagasy");
        LanguageID_TO_LanguageNAME.put("mt", "Maltese");
        LanguageID_TO_LanguageNAME.put("mo", "Moldavian");
        LanguageID_TO_LanguageNAME.put("mn", "Mongolian");
        LanguageID_TO_LanguageNAME.put("mi", "Maori");
        LanguageID_TO_LanguageNAME.put("ms", "Malay");
        LanguageID_TO_LanguageNAME.put("my", "Burmese");
        LanguageID_TO_LanguageNAME.put("na", "Nauru");
        LanguageID_TO_LanguageNAME.put("nv", "Navajo");
        LanguageID_TO_LanguageNAME.put("nr", "Ndebele");
        LanguageID_TO_LanguageNAME.put("nd", "Ndebele");
        LanguageID_TO_LanguageNAME.put("ng", "Ndonga");
        LanguageID_TO_LanguageNAME.put("ne", "Nepali");
        LanguageID_TO_LanguageNAME.put("nl", "Dutch");
        LanguageID_TO_LanguageNAME.put("nn", "Norwegian Nynorsk");
        LanguageID_TO_LanguageNAME.put("nb", "Norwegian Bokmål");
        LanguageID_TO_LanguageNAME.put("no", "Norwegian");
        LanguageID_TO_LanguageNAME.put("ny", "Chichewa; Nyanja");
        LanguageID_TO_LanguageNAME.put("oc", "Occitan");
        LanguageID_TO_LanguageNAME.put("oj", "Ojibwa");
        LanguageID_TO_LanguageNAME.put("or", "Oriya");
        LanguageID_TO_LanguageNAME.put("om", "Oromo");
        LanguageID_TO_LanguageNAME.put("os", "Ossetian; Ossetic");
        LanguageID_TO_LanguageNAME.put("pa", "Punjabi");
        LanguageID_TO_LanguageNAME.put("pi", "Pali");
        LanguageID_TO_LanguageNAME.put("pl", "Polish");
        LanguageID_TO_LanguageNAME.put("pt", "Portuguese");
        LanguageID_TO_LanguageNAME.put("qu", "Quechua");
        LanguageID_TO_LanguageNAME.put("rm", "Raeto-Romance");
        LanguageID_TO_LanguageNAME.put("ro", "Romanian");
        LanguageID_TO_LanguageNAME.put("rn", "Rundi");
        LanguageID_TO_LanguageNAME.put("ru", "Russian");
        LanguageID_TO_LanguageNAME.put("sg", "Sango");
        LanguageID_TO_LanguageNAME.put("sa", "Sanskrit");
        LanguageID_TO_LanguageNAME.put("si", "Sinhalese");
        LanguageID_TO_LanguageNAME.put("sk", "Slovak");
        LanguageID_TO_LanguageNAME.put("sl", "Slovenian");
        LanguageID_TO_LanguageNAME.put("se", "Northern Sami");
        LanguageID_TO_LanguageNAME.put("sm", "Samoan");
        LanguageID_TO_LanguageNAME.put("sn", "Shona");
        LanguageID_TO_LanguageNAME.put("sd", "Sindhi");
        LanguageID_TO_LanguageNAME.put("so", "Somali");
        LanguageID_TO_LanguageNAME.put("st", "Sotho");
        LanguageID_TO_LanguageNAME.put("es", "Spanish");
        LanguageID_TO_LanguageNAME.put("sq", "Albanian");
        LanguageID_TO_LanguageNAME.put("sc", "Sardinian");
        LanguageID_TO_LanguageNAME.put("sr", "Serbian");
        LanguageID_TO_LanguageNAME.put("ss", "Swati");
        LanguageID_TO_LanguageNAME.put("su", "Sundanese");
        LanguageID_TO_LanguageNAME.put("sw", "Swahili");
        LanguageID_TO_LanguageNAME.put("sv", "Swedish");
        LanguageID_TO_LanguageNAME.put("ty", "Tahitian");
        LanguageID_TO_LanguageNAME.put("ta", "Tamil");
        LanguageID_TO_LanguageNAME.put("tt", "Tatar");
        LanguageID_TO_LanguageNAME.put("te", "Telugu");
        LanguageID_TO_LanguageNAME.put("tg", "Tajik");
        LanguageID_TO_LanguageNAME.put("tl", "Tagalog");
        LanguageID_TO_LanguageNAME.put("th", "Thai");
        LanguageID_TO_LanguageNAME.put("ti", "Tigrinya");
        LanguageID_TO_LanguageNAME.put("to", "Tonga");
        LanguageID_TO_LanguageNAME.put("tn", "Tswana");
        LanguageID_TO_LanguageNAME.put("ts", "Tsonga");
        LanguageID_TO_LanguageNAME.put("tk", "Turkmen");
        LanguageID_TO_LanguageNAME.put("tr", "Turkish");
        LanguageID_TO_LanguageNAME.put("tw", "Twi");
        LanguageID_TO_LanguageNAME.put("ug", "Uighur");
        LanguageID_TO_LanguageNAME.put("uk", "Ukrainian");
        LanguageID_TO_LanguageNAME.put("ur", "Urdu");
        LanguageID_TO_LanguageNAME.put("uz", "Uzbek");
        LanguageID_TO_LanguageNAME.put("ve", "Venda");
        LanguageID_TO_LanguageNAME.put("vi", "Vietnamese");
        LanguageID_TO_LanguageNAME.put("vo", "Volapük");
        LanguageID_TO_LanguageNAME.put("wa", "Walloon");
        LanguageID_TO_LanguageNAME.put("wo", "Wolof");
        LanguageID_TO_LanguageNAME.put("xh", "Xhosa");
        LanguageID_TO_LanguageNAME.put("yi", "Yiddish");
        LanguageID_TO_LanguageNAME.put("za", "Zhuang");
        LanguageID_TO_LanguageNAME.put("zu", "Zulu");
        LanguageID_TO_LanguageNAME.put("ab", "Abkhazian");
        LanguageID_TO_LanguageNAME.put("zh", "Mandarin");
        LanguageID_TO_LanguageNAME.put("ps", "Pushto");
        LanguageID_TO_LanguageNAME.put("am", "Amharic");
        LanguageID_TO_LanguageNAME.put("ar", "Arabic");
        LanguageID_TO_LanguageNAME.put("be", "Belarusian");
        LanguageID_TO_LanguageNAME.put("bg", "Bulgarian");
        LanguageID_TO_LanguageNAME.put("cn", "Cantonese");
        LanguageID_TO_LanguageNAME.put("mk", "Macedonian");
        LanguageID_TO_LanguageNAME.put("ee", "Ewe");
        LanguageID_TO_LanguageNAME.put("el", "Greek");
        LanguageID_TO_LanguageNAME.put("fa", "Persian");
        LanguageID_TO_LanguageNAME.put("he", "Hebrew");
        LanguageID_TO_LanguageNAME.put("hi", "Hindi");
        LanguageID_TO_LanguageNAME.put("hy", "Armenian");
        LanguageID_TO_LanguageNAME.put("yo", "Yoruba");
    }

    @Autowired
    public TMDbService(TMDbConfig tmdbConfig, RestTemplate restTemplate) {
        this.tmdbConfig = tmdbConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Search for movies in TMDb API
     *
     * @param searchParams Movie object containing search parameters searchparams are of type title: string, genres: List<string> year: integer, actors: List<long>, directors: List<long>
     *                     searchparams cannot be spokenlanguages: List<string>!
     * @return List of movies matching the search criteria
     */
    public List<Movie> searchMovies(Movie searchParams) {
        try {
            // Don't search if no API key is configured
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.info("TMDB API key is not configured. Skipping external search.");
                return Collections.emptyList();
            }

            try {
                List<Movie> resultMovies = new ArrayList<>();
                Set<Long> processedMovieIds = new HashSet<>();

                int page = 1;
                int maxMovies = 100; // Maximum number of movies to return
                boolean hasMorePages = true;

                // Setup authentication headers
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(tmdbConfig.getApiKey());
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                while (hasMorePages && resultMovies.size() < maxMovies) {
                    if (searchParams.getTitle() != null) {
                        String searchEndpoint = tmdbConfig.getBaseUrl() + "/search/movie";
                        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(searchEndpoint)
                                .queryParam("sort_by", "popularity.desc")
                                .queryParam("page", page);

                        builder.queryParam("query", searchParams.getTitle().trim());

                        // Make the API call
                        ResponseEntity<String> response = restTemplate.exchange(
                                builder.toUriString(),
                                HttpMethod.GET,
                                entity,
                                String.class);

                        // Parse the response
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            JsonNode root = objectMapper.readTree(response.getBody());
                            JsonNode results = root.path("results");

                            // Check if there are more pages
                            int totalPages = root.path("total_pages").asInt();
                            hasMorePages = page < totalPages;


                            for (JsonNode movieNode : results) {
                                Movie movie = mapTMDbMovieToEntity(movieNode);
                                if (movie.getPosterURL() != null && movie.getMovieId() != 0 && movie.getTitle() != null) {
                                    // Only add movie if it hasn't been processed before
                                    if (!processedMovieIds.contains(movie.getMovieId())) {
                                        resultMovies.add(movie);
                                        processedMovieIds.add(movie.getMovieId());  // Track that we've processed this movie
                                    }


                                }
                            }

                            page++;

                        } else {
                            log.error("Error fetching movies from TMDb: {}", response.getStatusCode());
                            hasMorePages = false;
                        }

                    } else if (searchParams.getTitle() == null) {
                        String searchEndpoint = tmdbConfig.getBaseUrl() + "/discover/movie";
                        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(searchEndpoint)
                                .queryParam("sort_by", "popularity.desc")
                                .queryParam("page", page);

                        // Add search parameters if available
                        // years
                        if (searchParams.getYear() != null) {
                            builder.queryParam("primary_release_year", searchParams.getYear());
                        }

                        // genre
                        if (searchParams.getGenres() != null && !searchParams.getGenres().isEmpty()) {
                            // Convert genre names to IDs and join with comma
                            String genreIds = searchParams.getGenres().stream()
                                    .map(genre -> GENRE_NAME_TO_ID.getOrDefault(genre, ""))
                                    .filter(id -> !id.isEmpty())
                                    .collect(Collectors.joining(","));

                            if (!genreIds.isEmpty()) {
                                builder.queryParam("with_genres", genreIds);
                            }
                        }

                        // actor
                        if (searchParams.getActors() != null && !searchParams.getActors().isEmpty()) {
                            builder.queryParam("with_cast", searchParams.getActors());
                        }

                        // director
                        if (searchParams.getDirectors() != null && !searchParams.getDirectors().isEmpty()) {
                            builder.queryParam("with_crew", searchParams.getDirectors());
                        }

                        // Make the API call
                        ResponseEntity<String> response = restTemplate.exchange(
                                builder.toUriString(),
                                HttpMethod.GET,
                                entity,
                                String.class);

                        // Parse the response
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            JsonNode root = objectMapper.readTree(response.getBody());
                            JsonNode results = root.path("results");

                            // Check if there are more pages
                            int totalPages = root.path("total_pages").asInt();
                            hasMorePages = page < totalPages;


                            for (JsonNode movieNode : results) {
                                Movie movie = mapTMDbMovieToEntity(movieNode);
                                if (movie.getPosterURL() != null && movie.getMovieId() != 0 && movie.getTitle() != null) {
                                    // Only add movie if it hasn't been processed before
                                    if (!processedMovieIds.contains(movie.getMovieId())) {
                                        resultMovies.add(movie);
                                        processedMovieIds.add(movie.getMovieId());  // Track that we've processed this movie
                                    }
                                }
                            }

                            page++;

                        } else {
                            log.error("Error fetching movies from TMDb: {}", response.getStatusCode());
                            hasMorePages = false;
                        }
                    }  else {
                        return Collections.emptyList();
                    }
                }
                return resultMovies;
            } catch (RestClientException e) {
                log.error("Error searching for movies: {}", e.getMessage());
                return Collections.emptyList();
            }
            catch (Exception e) {
                log.error("Unexpected error during searching for movies: {}", e.getMessage());
                return Collections.emptyList();
            }
        }
        catch (Exception e) {
            log.error("Unexpected error during TMDb search: {}", e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     * Get detailed movie information from TMDb API
     *
     * @param movieId TMDb movie ID
     * @return Movie entity with detailed information
     */
    public Movie getMovieDetails(long movieId) {
        try {
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.warn("TMDB API key is not configured. Cannot get movie details.");
                return null;
            }

            String detailsEndpoint = tmdbConfig.getBaseUrl() + "/movie/" + movieId;

            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(detailsEndpoint)
                    .queryParam("append_to_response", "videos,credits");


            // Setup authentication headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tmdbConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    String.class);
            log.info("response: {}", response);


            if (response.getStatusCode() == HttpStatus.OK) {
                return parseMovieDetails(response.getBody());
            } else {
                log.error("Failed to get movie details from TMDb API: {}", response.getStatusCode());
                return null;
            }
        }
        catch (Exception e) {
            log.error("Error getting movie details from TMDb: {}", e.getMessage());
            return null;
        }
    }

    private Movie parseMovieDetails(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);

            Movie movie = mapTMDbMovieToEntity(rootNode);

            if (movie == null) {
                return null;
            }

            // Additional details not handled by mapTMDbMovieToEntity

            JsonNode spokenlanguagesNode = rootNode.path("spoken_languages");
            List<String> spokenlanguages = new ArrayList<>();
            if (spokenlanguagesNode.isArray()) {
                for (JsonNode languageNode : spokenlanguagesNode) {
                    if (languageNode.has("name")) {
                        spokenlanguages.add(languageNode.get("name").asText().trim());
                    }
                }
            }
            movie.setSpokenlanguages(spokenlanguages);

            JsonNode creditsNode = rootNode.path("credits");

            // Extract actors from cast and crew
            List<String> actors = extractTopActors(creditsNode);
            movie.setActors(actors);

            // Extract directors from crew
            List<String> directors = extractTopDirectors(creditsNode);
            movie.setDirectors(directors);

            JsonNode videosNode = rootNode.path("videos");
            String trailerURL = extractVideoURL(videosNode);
            if (trailerURL != null) {
                movie.setTrailerURL(trailerURL);
            }

            log.info("Movie to be saved - ID: {}, Title: {}, Actors: {}, Directors: {}",
                    movie.getMovieId(), movie.getTitle(), movie.getActors(), movie.getDirectors());

            return movie;
        } catch (Exception e) {
            log.error("Error parsing movie details: {}", e.getMessage());
            return null;
        }


    }

    /**
     * Extracts the YouTube video URL from a movie response with priority ordering:
     * 1. First YouTube trailer
     * 2. First YouTube teaser if no trailer exists
     * 3. First YouTube video of any type if no trailer or teaser exists
     *
     * @param videosNode the JSON response from TMDb API containing video information
     * @return the YouTube URL for the movie video, or null if none is found
     */
    public String extractVideoURL(JsonNode videosNode) {
        if (videosNode == null) {
            return null;
        }

        JsonNode videos = videosNode.get("results");
        if (videos == null || videos.isEmpty()) {
            return null;
        }

        // First priority: YouTube trailer
        String youtubeTrailerUrl = findYouTubeVideoByType(videos, "Trailer");
        if (youtubeTrailerUrl != null) {
            return youtubeTrailerUrl;
        }

        // Second priority: YouTube teaser
        String youtubeTeaserUrl = findYouTubeVideoByType(videos, "Teaser");
        if (youtubeTeaserUrl != null) {
            return youtubeTeaserUrl;
        }

        // Third priority: Any YouTube video
        for (JsonNode video : videos) {
            if (video.has("site") && "YouTube".equals(video.get("site").asText()) &&
                    video.has("key") && !video.get("key").asText().isEmpty()) {
                return "https://www.youtube.com/watch?v=" + video.get("key").asText();
            }
        }

        return null;  // No YouTube videos found
    }

    /**
     * Helper method to find a YouTube video of a specific type
     *
     * @param videos JSON array of videos
     * @param type the desired video type (e.g., "Trailer", "Teaser")
     * @return YouTube URL for the first matching video, or null if none found
     */
    private String findYouTubeVideoByType(JsonNode videos, String type) {
        for (JsonNode video : videos) {
            if (video.has("site") && "YouTube".equals(video.get("site").asText()) &&
                    video.has("type") && type.equals(video.get("type").asText()) &&
                    video.has("key") && !video.get("key").asText().isEmpty()) {
                return "https://www.youtube.com/watch?v=" + video.get("key").asText();
            }
        }
        return null;
    }


    /**
     * Extracts top actors from the cast and crew nodes of the TMDb API response.
     * Actors are identified by "known_for_department": "Acting"
     * Results are ordered by popularity in descending order.
     */
    private List<String> extractTopActors(JsonNode creditsNode) {


        if (creditsNode == null || creditsNode.isMissingNode()) {
            log.warn("Credits node is missing - returning empty list");
            return Collections.emptyList();
        }

        List<ActorData> actors = new ArrayList<>();

        // Extract actors from cast node if available
        JsonNode castNode = creditsNode.path("cast");
        if (castNode != null && !castNode.isMissingNode() && castNode.isArray()) {
            for (JsonNode actor : castNode) {
                if (actor.has("known_for_department") &&
                        "Acting".equals(actor.get("known_for_department").asText())) {

                    String name = actor.has("name") ? actor.get("name").asText() : "";
                    double popularity = actor.has("popularity") ? actor.get("popularity").asDouble() : 0.0;

                    actors.add(new ActorData(name, popularity));
                }
            }
        }

        // Extract actors from crew node if available (some actors might also be in crew)
        JsonNode crewNode = creditsNode.path("crew");
        if (crewNode != null && !crewNode.isMissingNode() && crewNode.isArray()) {
            for (JsonNode crewMember : crewNode) {
                if (crewMember.has("known_for_department") &&
                        "Acting".equals(crewMember.get("known_for_department").asText())) {

                    String name = crewMember.has("name") ? crewMember.get("name").asText() : "";
                    double popularity = crewMember.has("popularity") ? crewMember.get("popularity").asDouble() : 0.0;

                    // Only add if not already added from cast
                    if (actors.stream().noneMatch(a -> a.name.equals(name))) {
                        actors.add(new ActorData(name, popularity));
                    }
                }
            }
        }

        // Sort by popularity (descending)
        actors.sort((a1, a2) -> Double.compare(a2.popularity, a1.popularity));

        // Extract only names
        List<String> actorNames = actors.stream()
                .map(a -> a.name)
                .collect(Collectors.toList());

        return actorNames;
    }

    /**
     * Extracts top directors from the credits node of the TMDb API response.
     * Directors are identified by "known_for_department": "Directing" and "job": "Director"
     * Results are ordered by popularity in descending order.
     */
    private List<String> extractTopDirectors(JsonNode creditsNode) {
        if (creditsNode == null || creditsNode.isMissingNode()) {
            log.warn("Credits node is missing - returning empty list");
            return Collections.emptyList();
        }

        JsonNode crewNode = creditsNode.path("crew");
        if (crewNode == null || crewNode.isMissingNode() || !crewNode.isArray()) {
            log.warn("Crew node is missing - returning empty list");
            return Collections.emptyList();
        }

        List<DirectorData> directors = new ArrayList<>();

        for (JsonNode crewMember : crewNode) {
            if (crewMember.has("known_for_department") &&
                    "Directing".equals(crewMember.get("known_for_department").asText()) &&
                    crewMember.has("job") &&
                    "Director".equals(crewMember.get("job").asText())) {

                String name = crewMember.has("name") ? crewMember.get("name").asText() : "";
                double popularity = crewMember.has("popularity") ? crewMember.get("popularity").asDouble() : 0.0;

                directors.add(new DirectorData(name, popularity));
            }
        }

        // Sort by popularity (descending)
        directors.sort((d1, d2) -> Double.compare(d2.popularity, d1.popularity));

        // Extract only names
        List<String> directorNames = directors.stream()
                .map(d -> d.name)
                .collect(Collectors.toList());

        return directorNames;
    }

    // Helper classes for sorting by popularity
    private static class ActorData {
        final String name;
        final double popularity;

        public ActorData(String name, double popularity) {
            this.name = name;
            this.popularity = popularity;
        }
    }

    private static class DirectorData {
        final String name;
        final double popularity;

        public DirectorData(String name, double popularity) {
            this.name = name;
            this.popularity = popularity;
        }
    }

    /**
     * Get list of all genres from TMDb
     * Uses caching to avoid redundant API calls
     */
    private JsonNode cachedGenres = null;
    private long genresCacheTimestamp = 0;
    private static final long GENRE_CACHE_TTL = 86400000; // 24 hours in milliseconds
    
    public JsonNode getGenres() {
        try {
            // Return cached genres if available and not expired
            long currentTime = System.currentTimeMillis();
            if (cachedGenres != null && (currentTime - genresCacheTimestamp) < GENRE_CACHE_TTL) {
                return cachedGenres;
            }
            
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.warn("TMDB API key is not configured. Cannot get genres.");
                return null;
            }

            String genresEndpoint = tmdbConfig.getBaseUrl() + "/genre/movie/list";

            // Setup authentication headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tmdbConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    genresEndpoint,
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode genres = root.path("genres");
                
                // Cache the result
                cachedGenres = genres;
                genresCacheTimestamp = currentTime;
                
                return genres;
            }

            return null;
        }
        catch (Exception e) {
            log.error("Error getting genres from TMDb: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validates if a genre name or ID exists in TMDb genre list
     */
    public boolean isValidGenre(String genreName) {
        JsonNode genres = getGenres();
        
        if (genres == null || !genres.isArray()) {
            return false;
        }
        
        for (JsonNode genre : genres) {
            if (genre.has("name") && genre.get("name").asText().equals(genreName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get a genre ID from a genre name
     */
    public Integer getGenreIdByName(String genreName) {
        JsonNode genres = getGenres();
        
        if (genres == null || !genres.isArray()) {
            return null;
        }
        
        for (JsonNode genre : genres) {
            if (genre.has("name") && genre.get("name").asText().equals(genreName)) {
                return genre.get("id").asInt();
            }
        }
        
        return null;
    }

    /**
     * Maps TMDb API response to Movie entity
     */
    private Movie mapTMDbMovieToEntity(JsonNode movieData) {
        try {
            Movie movie = new Movie();

            movie.setMovieId(movieData.path("id").asLong());
            movie.setTitle(movieData.path("title").asText());
            movie.setDescription(movieData.path("overview").asText());

            // Extract release year from release_date (YYYY-MM-DD)
            String releaseDate = movieData.path("release_date").asText();
            if (releaseDate != null && !releaseDate.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(releaseDate, DateTimeFormatter.ISO_LOCAL_DATE);
                    movie.setYear(date.getYear());
                } catch (DateTimeParseException e) {
                    log.warn("Could not parse release date: {}", releaseDate);
                }
            }

            // Convert genre IDs to genre names
            JsonNode genreIdsNode = movieData.path("genre_ids");
            if (genreIdsNode.isArray()) {
                for (JsonNode genreIdNode : genreIdsNode) {
                    int genreId = genreIdNode.asInt();
                    String genreName = GENRE_ID_TO_NAME.getOrDefault(genreId, "Unknown");
                    movie.addGenre(genreName);
                }
            } else {
                JsonNode genresNode = movieData.path("genres");
                if (genresNode.isArray()) {
                    log.info("genresNode {}", genresNode.isArray());
                    for (JsonNode genreNode : genresNode) {
                        int genreId = genreNode.path("id").asInt();
                        String genreName = genreNode.path("name").asText();
                        // If name is provided directly, use it; otherwise look up in our map
                        if (genreName == null || genreName.isEmpty()) {
                            genreName = GENRE_ID_TO_NAME.getOrDefault(genreId, "Unknown");
                        }
                        movie.addGenre(genreName);
                    }
                }
            }

            // Set poster URL if available
            String posterPath = movieData.path("poster_path").asText(null);
            if (posterPath != null && !posterPath.isEmpty()) {
                movie.setPosterURL("https://image.tmdb.org/t/p/w500" + posterPath);
            }

            // Original language as the language using LanguageID_TO_LanguageNAME
            String languageCode = movieData.path("original_language").asText();
            String languageName = LanguageID_TO_LanguageNAME.getOrDefault(languageCode, languageCode);
            movie.setOriginallanguage(languageName);

            return movie;
        }
        catch (Exception e) {
            log.error("Error mapping TMDb movie to entity: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Search for actors by name
     *
     * @param query Actor name to search for
     * @return List of actors matching the search query
     * @throws RestClientException if the API request fails
     */
    public List<ActorDTO> searchActors(String query) {
        log.info("Searching for actors with query: {}", query);

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Actor search query cannot be empty");
        }

        try {
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.warn("TMDB API key is not configured. Cannot search for actors.");
                return Collections.emptyList();
            }

            String actorSearchEndpoint = tmdbConfig.getBaseUrl() + "/search/person";

            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(actorSearchEndpoint)
                    .queryParam("query", query)
                    .queryParam("language", "en-US");

            // Setup authentication headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tmdbConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Execute the request
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            // Process the response
            JsonNode responseBody = response.getBody();
            if (responseBody != null && responseBody.has("results")) {
                List<ActorDTO> actors = new ArrayList<>();
                for (JsonNode result : responseBody.get("results")) {
                    if (result.has("known_for_department") && "Acting".equals(result.get("known_for_department").asText())) {

                        ActorDTO actor = new ActorDTO();
                        actor.setActorId(result.get("id").asLong());
                        actor.setActorName(result.get("name").asText());
                        actors.add(actor);
                    }
                }
                return actors;
            }

            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error searching for actors: {}", e.getMessage());
            throw new RuntimeException("Error searching for actors: " + e.getMessage(), e);
        }
    }

    /**
     * Search for directors by name
     *
     * @param query Director name to search for
     * @return List of directors matching the search query
     * @throws RestClientException if the API request fails
     */
    public List<DirectorDTO> searchDirectors(String query) {
        log.info("Searching for directors with query: {}", query);

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Director search query cannot be empty");
        }

        try {
            if (tmdbConfig.getApiKey().isEmpty()) {
                log.warn("TMDB API key is not configured. Cannot search for directors.");
                return Collections.emptyList();
            }

            String directorSearchEndpoint = tmdbConfig.getBaseUrl() + "/search/person";

            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(directorSearchEndpoint)
                    .queryParam("query", query)
                    .queryParam("language", "en-US");

            // Setup authentication headers
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(tmdbConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Execute the request
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            // Process the response
            JsonNode responseBody = response.getBody();
            if (responseBody != null && responseBody.has("results")) {
                List<DirectorDTO> directors = new ArrayList<>();
                for (JsonNode result : responseBody.get("results")) {
                    if (result.has("known_for_department") && "Directing".equals(result.get("known_for_department").asText())) {
                        DirectorDTO director = new DirectorDTO();
                        director.setDirectorId(result.get("id").asLong());
                        director.setDirectorName(result.get("name").asText());
                        directors.add(director);
                    }
                }
                return directors;
            }

            return Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Error searching for directors: {}", e.getMessage());
            throw new RuntimeException("Error searching for directors: " + e.getMessage(), e);
        }
    }
}


