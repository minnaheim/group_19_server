package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

/**
 * DTO for saving and fetching user's favorite directors
 */
public class UserFavoritesDirectorsDTO {

    private List<String> favoriteDirectors;

    public List<String> getFavoriteDirectors() {
        return favoriteDirectors;
    }

    public void setFavoriteDirectors(List<String> favoriteDirectors) {
        this.favoriteDirectors = favoriteDirectors;
    }
}
