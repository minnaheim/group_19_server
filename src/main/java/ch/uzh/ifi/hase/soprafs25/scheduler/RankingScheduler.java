package ch.uzh.ifi.hase.soprafs25.scheduler;

import ch.uzh.ifi.hase.soprafs25.service.RankingService;
import ch.uzh.ifi.hase.soprafs25.repository.GroupRepository;
import ch.uzh.ifi.hase.soprafs25.entity.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RankingScheduler {

    private final Logger log = LoggerFactory.getLogger(RankingScheduler.class);

    private final RankingService rankingService;
    private final GroupRepository groupRepository;

    /**
     * Constructor for dependency injection.
     * @param rankingService The service responsible for ranking logic.
     * @param groupRepository Repository to access group data.
     */
    @Autowired
    public RankingScheduler(RankingService rankingService, GroupRepository groupRepository) {
        this.rankingService = rankingService;
        this.groupRepository = groupRepository;
    }

    /**
     * Scheduled task to calculate the ranking winner.
     * Runs periodically based on the cron expression defined in application.properties 
     * (sopra.scheduling.ranking.cron).
     * Fetches all groups and calculates the winner for each.
     */
    @Scheduled(cron = "${sopra.scheduling.ranking.cron:0 0 0 * * ?}") // Default to midnight daily if property not set
    public void calculateWinner() {
        log.info("Ranking calculation scheduler started.");
        List<Group> allGroups = groupRepository.findAll();

        if (allGroups.isEmpty()) {
            log.info("No groups found. Skipping ranking calculation.");
            return;
        }

        log.info("Calculating ranking winners for {} groups.", allGroups.size());
        for (Group group : allGroups) {
            try {
                log.debug("Calculating winner for group ID: {}", group.getGroupId());
                rankingService.calculateAndSaveWinner(group.getGroupId());
            } catch (Exception e) {
                // Log error for specific group but continue with others
                log.error("Error calculating winner for group ID {}: {}", group.getGroupId(), e.getMessage(), e);
            }
        }
        log.info("Ranking calculation scheduler finished.");
    }
}
