package ch.uzh.ifi.hase.soprafs25.rest.mapper;

import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for creating information (POST).
 */
@Mapper
public interface DTOMapper {

    DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "password", target = "password")
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "token", target = "token")
    @Mapping(source = "bio", target = "bio")
    @Mapping(source = "favoriteGenres", target = "favoriteGenres")
    @Mapping(source = "favoriteActors", target = "favoriteActors")
    @Mapping(source = "favoriteDirectors", target = "favoriteDirectors")
    @Mapping(source = "watchlist", target = "watchlist")
    @Mapping(source = "watchedMovies", target = "watchedMovies")
    @Mapping(source = "status", target = "status")
    UserGetDTO convertEntityToUserGetDTO(User user);

    @Mapping(source = "movieId", target = "movieId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "genres", target = "genres")
    @Mapping(source = "year", target = "year")
    @Mapping(source = "actors", target = "actors")
    @Mapping(source = "directors", target = "directors")
    @Mapping(source = "originallanguage", target = "originallanguage")
    @Mapping(source = "trailerURL", target = "trailerURL")
    @Mapping(source = "posterURL", target = "posterURL")
    @Mapping(source = "description", target = "description")
    Movie convertMovieGetDTOtoEntity(MovieGetDTO movieGetDTO);

    @Mapping(source = "movieId", target = "movieId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "genres", target = "genres")
    @Mapping(source = "year", target = "year")
    @Mapping(source = "actors", target = "actors")
    @Mapping(source = "directors", target = "directors")
    @Mapping(source = "originallanguage", target = "originallanguage")
    @Mapping(source = "posterURL", target = "posterURL")
    @Mapping(source = "trailerURL", target = "trailerURL")
    @Mapping(source = "description", target = "description")
    MovieGetDTO convertEntityToMovieGetDTO(Movie movie);
}
