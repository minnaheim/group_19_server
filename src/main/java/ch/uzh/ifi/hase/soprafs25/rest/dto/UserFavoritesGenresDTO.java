package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

public class UserFavoritesGenresDTO {
    private List<String> genreIds;

    public List<String> getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(List<String> genreIds) {
        this.genreIds = genreIds;
    }
}
