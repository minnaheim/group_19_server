package ch.uzh.ifi.hase.soprafs25.scheduler;

import ch.uzh.ifi.hase.soprafs25.service.RankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RankingScheduler {

    private final Logger log = LoggerFactory.getLogger(RankingScheduler.class);

    private final RankingService rankingService;

    @Autowired
    public RankingScheduler(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /**
     * Scheduled task to calculate and save the winner of the movie ranking.
     * Runs daily at midnight (00:00:00).
     * Cron format: second, minute, hour, day of month, month, day(s) of week
     * See: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html
     */
    @Scheduled(cron = "0 0 0 * * *") // Runs daily at midnight
    // For testing, you might use a faster rate, e.g., every 5 minutes: @Scheduled(cron = "0 */5 * * * *")
    // Or a fixed rate: @Scheduled(fixedRate = 300000) // Every 5 minutes (in milliseconds)
    public void triggerWinnerCalculation() {
        log.info("Scheduled task: Triggering winner calculation...");
        try {
            rankingService.calculateAndSaveWinner();
            log.info("Scheduled task: Winner calculation completed successfully.");
        } catch (Exception e) {
            log.error("Scheduled task: Error during winner calculation", e);
            // Consider adding more robust error handling/notifications if needed
        }
    }
}
