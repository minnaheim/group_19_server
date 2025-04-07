package ch.uzh.ifi.hase.soprafs25.rest.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs25.entity.Group;
import ch.uzh.ifi.hase.soprafs25.entity.Movie;
import ch.uzh.ifi.hase.soprafs25.entity.User;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.GroupPostDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.MovieGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs25.rest.dto.UserPostDTO;

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
    @Mapping(source = "genre", target = "genre")
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
    @Mapping(source = "genre", target = "genre")
    @Mapping(source = "year", target = "year")
    @Mapping(source = "actors", target = "actors")
    @Mapping(source = "directors", target = "directors")
    @Mapping(source = "originallanguage", target = "originallanguage")
    @Mapping(source = "posterURL", target = "posterURL")
    @Mapping(source = "trailerURL", target = "trailerURL")
    @Mapping(source = "description", target = "description")
    MovieGetDTO convertEntityToMovieGetDTO(Movie movie);
    
    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "moviePool", ignore = true)
    @Mapping(source = "groupName", target = "groupName")
    Group convertGroupPostDTOtoEntity(GroupPostDTO groupPostDTO);

    @Mapping(source = "groupId", target = "groupId")
    @Mapping(source = "groupName", target = "groupName")
    @Mapping(source = "creator.userId", target = "creatorId")
    @Mapping(source = "members", target = "memberIds")
    @Mapping(source = "moviePool", target = "movieIds")
    GroupGetDTO convertEntityToGroupGetDTO(Group group);

    default List<Long> mapUsersToIds(List<User> users) {
      if (users == null) return null;
      return users.stream()
                 .map(User::getUserId)
                 .collect(Collectors.toList());
    }

    default List<Long> mapMoviesToIds(List<Movie> movies) {
      if (movies == null) return null;
      return movies.stream()
                  .map(Movie::getMovieId)
                  .collect(Collectors.toList());
    }
}

