package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

/**
 * DTO for saving and fetching user's favorite directors
 */
public class UserFavoritesDirectorsDTO {

    private List<DirectorDTO> favoriteDirectors;

    public List<DirectorDTO> getFavoriteDirectors() {
        return favoriteDirectors;
    }

    public void setFavoriteDirectors(List<DirectorDTO> favoriteDirectors) {
        this.favoriteDirectors = favoriteDirectors;
    }
}
