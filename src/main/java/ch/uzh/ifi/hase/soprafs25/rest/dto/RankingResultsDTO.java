package ch.uzh.ifi.hase.soprafs25.rest.dto;

import java.util.List;

public class RankingResultsDTO {
    private Long resultId;
    private Long groupId;
    private String calculatedAt;
    private MovieRankGetDTO winningMovie;
    private int numberOfVoters;
    private List<MovieAverageRankDTO> detailedResults;

    public Long getResultId() { return resultId; }
    public void setResultId(Long resultId) { this.resultId = resultId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(String calculatedAt) { this.calculatedAt = calculatedAt; }

    public MovieRankGetDTO getWinningMovie() { return winningMovie; }
    public void setWinningMovie(MovieRankGetDTO winningMovie) { this.winningMovie = winningMovie; }

    public int getNumberOfVoters() { return numberOfVoters; }
    public void setNumberOfVoters(int numberOfVoters) { this.numberOfVoters = numberOfVoters; }

    public List<MovieAverageRankDTO> getDetailedResults() { return detailedResults; }
    public void setDetailedResults(List<MovieAverageRankDTO> detailedResults) { this.detailedResults = detailedResults; }
}
