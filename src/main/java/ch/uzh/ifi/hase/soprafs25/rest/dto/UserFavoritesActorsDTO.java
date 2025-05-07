package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

/**
 * DTO for saving and fetching user's favorite actors
 */
public class UserFavoritesActorsDTO {

    private List<ActorDTO> favoriteActors;

    public List<ActorDTO> getFavoriteActors() {
        return favoriteActors;
    }

    public void setFavoriteActors(List<ActorDTO> favoriteActors) {
        this.favoriteActors = favoriteActors;
    }
}
