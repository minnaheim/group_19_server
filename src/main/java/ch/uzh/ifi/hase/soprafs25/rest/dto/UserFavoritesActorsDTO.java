package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

/**
 * DTO for saving and fetching user's favorite actors
 */
public class UserFavoritesActorsDTO {

    private List<String> favoriteActors;

    public List<String> getFavoriteActors() {
        return favoriteActors;
    }

    public void setFavoriteActors(List<String> favoriteActors) {
        this.favoriteActors = favoriteActors;
    }
}
