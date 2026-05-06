package tn.esprit.gestionclasses.Services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Créneaux récurrents hebdomadaires. Codes stables pour API / stockage.
 */
public final class SessionSlotRegistry {

    private SessionSlotRegistry() {}

    /** Codes autorisés (mardi/mercredi 17h–20h et 19h–22h ; samedi/dimanche 9h–12h, 14h–17h, 17h–20h). */
    public static final Set<String> ALLOWED_CODES = Set.of(
            "TUE_17_20",
            "TUE_19_22",
            "WED_17_20",
            "WED_19_22",
            "SAT_09_12",
            "SAT_14_17",
            "SAT_17_20",
            "SUN_09_12",
            "SUN_14_17",
            "SUN_17_20"
    );

    private static final Map<String, String> DAY_PREFIX = new HashMap<>();

    static {
        for (String code : ALLOWED_CODES) {
            int i = code.indexOf('_');
            if (i > 0) {
                DAY_PREFIX.put(code, code.substring(0, i));
            }
        }
    }

    public static String dayKey(String slotCode) {
        return DAY_PREFIX.get(slotCode);
    }

    public static Set<String> allowedCodesView() {
        return Collections.unmodifiableSet(ALLOWED_CODES);
    }
}
