package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

public class VoteStateGetDTO {

    private List<MovieGetDTO> pool;
    private List<RankingSubmitDTO> rankings;

    public List<MovieGetDTO> getPool() {
        return pool;
    }

    public void setPool(List<MovieGetDTO> pool) {
        this.pool = pool;
    }

    public List<RankingSubmitDTO> getRankings() {
        return rankings;
    }

    public void setRankings(List<RankingSubmitDTO> rankings) {
        this.rankings = rankings;
    }
}
