package com.liftsync.config.cache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.liftsync.dto.response.athlete.AthletePersonalInfoDTO;
import com.liftsync.dto.response.block.BlockTemplatePreviewDTO;
import com.liftsync.dto.response.block.ModalPreviewBlockDTO;
import com.liftsync.dto.response.coach.CoachProfileDTO;
import com.liftsync.dto.response.metrics.AthleteMetricsResp;
import com.liftsync.dto.response.questionnaires.QuestionnaireTemplateDTO;
import com.liftsync.dto.response.user.UserDTO;
import com.liftsync.dto.response.webrating.WebRatingDTO;
import com.liftsync.model.ExerciseCatalog;
import com.liftsync.model.FoodItem;


public final class Caches {

    public static final String USER_DTO_BY_PUBLIC_ID = "userDTOByPublicId";
    public static final String ATHLETE_PERSONAL_INFO_DTO = "athletePersonalInfoDTO";
    public static final String ATHLETE_METRICS = "athleteMetrics";
    public static final String BLOCK_MODAL_PREVIEW = "blockModalPreview";
    public static final String ALL_COACHES_INFO = "allCoachesInfo";
    public static final String SEARCH_PENDING_COACHES = "searchPendingCoaches";
    public static final String HIGHEST_WEB_RATINGS = "highestWebRatings";
    public static final String COACH_SPECIALIZATIONS = "coachSpecializations";
    public static final String HIGH_INTENSITY_TECHNIQUES = "highIntensityTechniques";
    public static final String COACH_PUBLIC_ID_BY_ATHLETE_ID = "coachPublicIdByAthleteId";
    public static final String ALL_EXERCISES = "allExercises";
    public static final String ALL_FOOD_ITEMS = "allFoodItems";
    public static final String FOOD_ITEM_BY_NAME = "foodItemByName";
    public static final String QUESTIONNAIRE_LIBRARY_DEFAULTS = "questionnaireLibraryDefaults";
    public static final String QUESTIONNAIRE_LIBRARY_COACH = "questionnaireLibraryCoach";
    public static final String BLOCK_LIBRARY = "blockLibrary";


    private static final Map<String, CacheSpec<?>> REGISTRY = new LinkedHashMap<>();

    static {
        // DTO-backed caches
        register(CacheSpec.of(USER_DTO_BY_PUBLIC_ID, UserDTO.class));
        register(CacheSpec.of(ATHLETE_PERSONAL_INFO_DTO, AthletePersonalInfoDTO.class));
        register(CacheSpec.of(ATHLETE_METRICS, AthleteMetricsResp.class));
        register(CacheSpec.of(BLOCK_MODAL_PREVIEW, ModalPreviewBlockDTO.class));
        register(CacheSpec.ofList(ALL_COACHES_INFO, CoachProfileDTO.class));
        register(CacheSpec.ofList(SEARCH_PENDING_COACHES, UserDTO.class));
        register(CacheSpec.ofList(HIGHEST_WEB_RATINGS, WebRatingDTO.class));
        register(CacheSpec.ofList(QUESTIONNAIRE_LIBRARY_DEFAULTS, QuestionnaireTemplateDTO.class));
        register(CacheSpec.ofList(QUESTIONNAIRE_LIBRARY_COACH, QuestionnaireTemplateDTO.class));
        register(CacheSpec.ofList(BLOCK_LIBRARY, BlockTemplatePreviewDTO.class));
        // Scalar / enum-backed
        register(CacheSpec.ofList(COACH_SPECIALIZATIONS, String.class));
        register(CacheSpec.ofMap(HIGH_INTENSITY_TECHNIQUES, String.class, String.class));
        register(CacheSpec.of(COACH_PUBLIC_ID_BY_ATHLETE_ID, String.class));

        // ENTITY caches — consider migrating to DTOs
        register(CacheSpec.ofList(ALL_EXERCISES, ExerciseCatalog.class));
        register(CacheSpec.ofList(ALL_FOOD_ITEMS, FoodItem.class));
        register(CacheSpec.ofList(FOOD_ITEM_BY_NAME, FoodItem.class));

        verifyEveryNameHasASpec();
    }

    // --- Public API -----------------------------------------------------------------------------

    /**
     * All registered specs, in declaration order.
     */
    public static Collection<CacheSpec<?>> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    // --- Internals ------------------------------------------------------------------------------

    private static void register(CacheSpec<?> spec) {
        if (REGISTRY.putIfAbsent(spec.name(), spec) != null) {
            throw new IllegalStateException("Duplicate cache definition: " + spec.name());
        }
    }

    /**
     * Reflectively asserts that every public {@code static final String} constant declared on this
     * class has a corresponding entry in {@link #REGISTRY}. Prevents silent drift where someone adds
     * a name constant but forgets the {@code register(...)} call.
     */
    private static void verifyEveryNameHasASpec() {
        List<String> missing = new ArrayList<>();

        for (Field field : Caches.class.getDeclaredFields()) {
            int mods = field.getModifiers();

            if (!(Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods))) {
                continue;
            }

            if (field.getType() != String.class) {
                continue;
            }

            try {
                String value = (String) field.get(null);
                if (!REGISTRY.containsKey(value)) {
                    missing.add(field.getName() + " (\"" + value + "\")");
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to read " + field.getName(), e);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Caches: the following name constants have no registered CacheSpec — "
                            + "add a register(...) call in the static initializer: " + missing);
        }
    }

    private Caches() {
    }
}
