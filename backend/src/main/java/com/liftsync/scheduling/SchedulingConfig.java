package com.liftsync.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.liftsync.util.EnvironmentUtil;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {

    private final BlockAutomation blockAutomation;
    private final QuestionnaireAutomation questionnaireAutomation;
    private final EnvironmentUtil environmentUtil;

    public SchedulingConfig(BlockAutomation blockAutomation,
                            QuestionnaireAutomation questionnaireAutomation,
                            EnvironmentUtil environmentUtil) {
        this.blockAutomation = blockAutomation;
        this.questionnaireAutomation = questionnaireAutomation;
        this.environmentUtil = environmentUtil;
    }

    /**
     * Runs every Friday at 00:00 (midnight) Europe/Madrid time
     */
    @Scheduled(cron = "0 0 0 ? * FRI", zone = "Europe/Madrid")
    public void scheduleWeeklyBlockClone() {
        blockAutomation.cloneCurrentWeeksForAllAthletes();
    }

    /**
     * Runs every day at 00:00 (midnight) Europe/Madrid time
     * Creates new daily questionnaire assignments for completed ones
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Madrid")
    public void scheduleDailyQuestionnaires() {
        questionnaireAutomation.processSchedules();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Madrid")
    public void setInactiveRoutines() {
        blockAutomation.setInactiveRoutines();
    }

    @Scheduled(cron = "0 0 3 * * ?", zone = "Europe/Madrid")
    public void setInactiveQuestionnaires() {
        questionnaireAutomation.setInactiveQuestionnaires();
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Madrid")
    public void scheduleBlocks(){
        blockAutomation.createBlocksScheduled();
    }

    public void runAllAutomations() {
        if(environmentUtil.isDevelopment()){
            blockAutomation.cloneCurrentWeeksForAllAthletes();
            blockAutomation.createBlocksScheduled();
            questionnaireAutomation.processSchedules();
            blockAutomation.setInactiveRoutines();
            questionnaireAutomation.setInactiveQuestionnaires();
        }

    }

}
