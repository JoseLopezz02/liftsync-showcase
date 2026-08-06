package com.liftsync.cache;

import com.liftsync.config.TransactionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import com.liftsync.config.cache.Caches;

@Component
public class CacheEvictor {

    private static final String ALL_SPECIALIZATIONS_KEY = "allSpecializations";
    private static final String ALL_COACHES_INFO_KEY = "allCoachesInfo";
    private static final String HIGHEST_RATINGS_KEY = "highestRatings";

    private final CacheManager cacheManager;
    private final TransactionManager transactionManager;

    public CacheEvictor(ObjectProvider<CacheManager> cacheManagerProvider, TransactionManager transactionManager) {
        this.cacheManager = cacheManagerProvider.getIfAvailable();
        this.transactionManager = transactionManager;
    }

    public void clearCache(String cacheName) {
        if (cacheManager == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void evict(String cacheName, Object key) {
        if (cacheManager == null || key == null) {
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    public void evictQuestionnaireLibraryByCoachIdAfterCommit(Long coachId) {
        transactionManager.runAfterCommit(() -> evictQuestionnaireLibraryByCoachId(coachId));
    }

    public void evictBlockLibraryByCoachIdAfterCommit(Long coachId) {
        transactionManager.runAfterCommit(() -> evictBlockLibraryByCoachId(coachId));
    }

    public void evictCoachPersonalInfoAfterCommit() {
        transactionManager.runAfterCommit(() -> {
            evictCoachSpecializations();
            evictAllCoachesInfo();
        });
    }

    public void evictHighestWebRatingsAfterCommit() {
        transactionManager.runAfterCommit(this::evictHighestWebRatings);
    }

    public void evictBlockModalPreviewByTemplateIdAfterCommit(Long templateId) {
        transactionManager.runAfterCommit(() -> evictBlockModalPreviewByTemplateId(templateId));
    }

    public void evictAthleteMetricsByAthleteAndCoachAfterCommit(Long athleteId, Long coachId) {
        transactionManager.runAfterCommit(() -> evictAthleteMetricsByAthleteAndCoach(athleteId, coachId));
    }

    public void evictCoachPublicIdByAthleteIdAfterCommit(Long athleteId) {
        transactionManager.runAfterCommit(() -> evictCoachPublicIdByAthleteId(athleteId));
    }

    public void evictUserDtoByIdAfterCommit(Long userId) {
        transactionManager.runAfterCommit(() -> evictUserDtoById(userId));
    }

    public void evictUserDtoByPublicIdAfterCommit(String publicId) {
        transactionManager.runAfterCommit(() -> evictUserDtoByPublicId(publicId));
    }

    public void evictAthletePersonalInfoByAthleteIdAfterCommit(Long athleteId) {
        transactionManager.runAfterCommit(() -> evictAthletePersonalInfoByAthleteId(athleteId));
    }


    // Function that only evicted without checking if there is any commited transaction //

    public void evictUserDtoByPublicId(String publicId) {
        evict(Caches.USER_DTO_BY_PUBLIC_ID, publicId);
    }

    public void evictUserDtoById(Long userId) {
        evict(Caches.USER_DTO_BY_ID, userId);
    }

    public void evictAthletePersonalInfoByAthleteId(Long athleteId) {
        evict(Caches.ATHLETE_PERSONAL_INFO_DTO, athleteId);
    }

    public void evictCoachSpecializations() {
        evict(Caches.COACH_SPECIALIZATIONS, ALL_SPECIALIZATIONS_KEY);
    }

    public void evictQuestionnaireLibraryByCoachId(Long coachId) {
        evict(Caches.QUESTIONNAIRE_LIBRARY_COACH, coachId);
    }

    public void evictAllCoachesInfo() {
        evict(Caches.ALL_COACHES_INFO, ALL_COACHES_INFO_KEY);
    }

    public void evictBlockLibraryByCoachId(Long coachId) {
        evict(Caches.BLOCK_LIBRARY, coachId);
    }

    public void evictHighestWebRatings() {
        evict(Caches.HIGHEST_WEB_RATINGS, HIGHEST_RATINGS_KEY);
    }

    public void evictBlockModalPreviewByTemplateId(Long templateId) {
        evict(Caches.BLOCK_MODAL_PREVIEW, templateId);
    }

    public void evictAthleteMetricsByAthleteAndCoach(Long athleteId, Long coachId) {
        evict(Caches.ATHLETE_METRICS, CacheKeyUtils.athleteMetrics(athleteId, coachId));
    }

    public void evictCoachPublicIdByAthleteId(Long athleteId) {
        evict(Caches.COACH_PUBLIC_ID_BY_ATHLETE_ID, athleteId);
    }

}
