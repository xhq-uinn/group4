package com.example.a1112;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MotivationCalculator {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onComplete();
    }

    // ============================================================
    // PUBLIC ENTRY
    // ============================================================
    public void updateAllMotivation(String childId, Callback callback) {

        db.collection("children").document(childId)
                .get()
                .addOnSuccessListener(childDoc -> {

                    if (!childDoc.exists()) {
                        Log.w("MOTIVATION", "Child does not exist");
                        callback.onComplete();
                        return;
                    }

                    String parentId = childDoc.getString("parentId");
                    if (parentId == null) parentId = "";

                    loadSettings(parentId, settings ->
                            loadEverything(childId, settings, callback));
                });
    }

    // ============================================================
    // SETTINGS
    // ============================================================
    private static class Settings {
        int controllerStreakTarget = 7;        // perfect week target
        int techniqueStreakTarget = 7;         // if needed
        int techniqueHighQualityCount = 10;    // high-quality threshold
        int lowRescueThreshold = 4;            // rescue days allowed
    }

    private interface SettingsCallback {
        void onLoaded(Settings s);
    }

    private void loadSettings(String parentId, SettingsCallback cb) {
        Settings s = new Settings();

        if (parentId.isEmpty()) {
            cb.onLoaded(s);
            return;
        }

        db.collection("users")
                .document(parentId)
                .collection("motivationSettings")
                .document("config")
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        if (doc.getLong("controllerStreakTarget") != null)
                            s.controllerStreakTarget = doc.getLong("controllerStreakTarget").intValue();
                        if (doc.getLong("techniqueStreakTarget") != null)
                            s.techniqueStreakTarget = doc.getLong("techniqueStreakTarget").intValue();
                        if (doc.getLong("techniqueHighQualityCount") != null)
                            s.techniqueHighQualityCount = doc.getLong("techniqueHighQualityCount").intValue();
                        if (doc.getLong("lowRescueThreshold") != null)
                            s.lowRescueThreshold = doc.getLong("lowRescueThreshold").intValue();
                    }

                    cb.onLoaded(s);
                })
                .addOnFailureListener(e -> {
                    Log.e("MOTIVATION", "Failed to load settings", e);
                    cb.onLoaded(s);
                });
    }

    // ============================================================
    // LOAD EVERYTHING
    // ============================================================
    private void loadEverything(String childId, Settings settings, Callback callback) {

        List<MedicineLog> controllerLogs = new ArrayList<>();
        List<MedicineLog> rescueLogs = new ArrayList<>();
        List<TechSession> techLogs = new ArrayList<>();
        final ControllerSchedule[] scheduleHolder = new ControllerSchedule[1];

        AtomicInteger loaded = new AtomicInteger(0);
        Runnable tryFinish = () -> {
            if (loaded.incrementAndGet() == 4) {
                computeAndSave(childId, settings, controllerLogs, rescueLogs, techLogs, scheduleHolder[0], callback);
            }
        };

        // Controller logs
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "controller")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot d : q)
                        controllerLogs.add(d.toObject(MedicineLog.class));
                    tryFinish.run();
                });

        // Rescue logs
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "rescue")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot d : q)
                        rescueLogs.add(d.toObject(MedicineLog.class));
                    tryFinish.run();
                });

        // Technique sessions
        db.collection("children")
                .document(childId)
                .collection("technique_sessions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot d : q)
                        techLogs.add(d.toObject(TechSession.class));
                    tryFinish.run();
                });

        // Controller schedule
        db.collection("controllerSchedules")
                .document(childId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists())
                        scheduleHolder[0] = doc.toObject(ControllerSchedule.class);
                    tryFinish.run();
                });
    }

    // ============================================================
    // MAIN COMPUTATION
    // ============================================================
    private void computeAndSave(
            String childId,
            Settings settings,
            List<MedicineLog> controllerLogs,
            List<MedicineLog> rescueLogs,
            List<TechSession> techLogs,
            ControllerSchedule schedule,
            Callback callback
    ) {

        Map<String, Integer> controllerMap = buildControllerDayMap(controllerLogs);
        Map<String, Boolean> techniqueCompleted = buildTechniqueCompletedMap(techLogs);
        Map<String, Boolean> techniqueHQ = buildTechniqueHQMap(techLogs);

        int controllerStreak = computeControllerStreak(schedule, controllerMap);
        int techniqueStreak = computeTechniqueStreak(schedule, techniqueCompleted);

        boolean perfectControllerWeek = computePerfectWeek(controllerLogs, settings);
        int highQualityTechniqueCount = countHighQuality(techLogs);
        boolean lowRescueMonth = checkLowRescue(settings, rescueLogs);

        Map<String, Object> data = new HashMap<>();
        data.put("controllerStreak", controllerStreak);
        data.put("techniqueStreak", techniqueStreak);
        data.put("perfectControllerWeek", perfectControllerWeek);
        data.put("highQualityTechniqueCount", highQualityTechniqueCount);
        data.put("lowRescueMonth", lowRescueMonth);
        data.put("lastUpdated", Timestamp.now());

        db.collection("children")
                .document(childId)
                .collection("motivation")
                .document("status")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onComplete());
    }

    // ============================================================
    // NORMALIZATION HELPERS
    // ============================================================
    private Map<String, Integer> buildControllerDayMap(List<MedicineLog> logs) {
        Map<String, Integer> map = new HashMap<>();
        Calendar c = Calendar.getInstance();

        for (MedicineLog m : logs) {
            c.setTime(m.getTimestamp());
            String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
            map.put(key, map.getOrDefault(key, 0) + m.getDoseCount());
        }
        return map;
    }

    private Map<String, Boolean> buildTechniqueCompletedMap(List<TechSession> logs) {
        Map<String, Boolean> map = new HashMap<>();
        Calendar c = Calendar.getInstance();

        for (TechSession t : logs) {
            if (!t.completed) continue;
            c.setTime(t.getDate());
            String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
            map.put(key, true);
        }
        return map;
    }

    private Map<String, Boolean> buildTechniqueHQMap(List<TechSession> logs) {
        Map<String, Boolean> map = new HashMap<>();
        Calendar c = Calendar.getInstance();

        for (TechSession t : logs) {
            if (!"high-quality".equalsIgnoreCase(t.quality)) continue;
            c.setTime(t.getDate());
            String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
            map.put(key, true);
        }
        return map;
    }

    // ============================================================
    // STREAK CALCULATION (FINAL RULE)
    // ============================================================
    private int computeControllerStreak(ControllerSchedule schedule, Map<String, Integer> controllerMap) {
        if (schedule == null) return 0;

        int streak = 0;

        Calendar cursor = Calendar.getInstance();
        cursor.set(Calendar.HOUR_OF_DAY, 0);
        cursor.set(Calendar.MINUTE, 0);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);

        while (true) {
            int dow = cursor.get(Calendar.DAY_OF_WEEK);
            int planned = schedule.getDoseForDay(dow);

            String key = cursor.get(Calendar.YEAR) + "-" + cursor.get(Calendar.DAY_OF_YEAR);
            int actual = controllerMap.getOrDefault(key, 0);

            if (planned == 0) {
                if (actual == 0) {
                    streak++; // success
                } else break; // failed zero-day
            } else {
                if (actual == planned) {
                    streak++;
                } else break;
            }

            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    private int computeTechniqueStreak(ControllerSchedule schedule, Map<String, Boolean> techniqueComplete) {
        if (schedule == null) return 0;

        int streak = 0;

        Calendar cursor = Calendar.getInstance();
        cursor.set(Calendar.HOUR_OF_DAY, 0);
        cursor.set(Calendar.MINUTE, 0);
        cursor.set(Calendar.SECOND, 0);
        cursor.set(Calendar.MILLISECOND, 0);

        while (true) {
            int dow = cursor.get(Calendar.DAY_OF_WEEK);
            int planned = schedule.getDoseForDay(dow);

            if (planned == 0) {
                cursor.add(Calendar.DAY_OF_YEAR, -1);
                continue;
            }

            String key = cursor.get(Calendar.YEAR) + "-" + cursor.get(Calendar.DAY_OF_YEAR);

            if (!techniqueComplete.getOrDefault(key, false))
                break;

            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }

        return streak;
    }

    // ============================================================
    // BADGE CONDITIONS
    // ============================================================
    private boolean computePerfectWeek(List<MedicineLog> logs, Settings s) {
        // Build day-completed map
        Set<String> completedDays = new HashSet<>();
        Calendar c = Calendar.getInstance();

        for (MedicineLog m : logs) {
            c.setTime(m.getTimestamp());
            String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
            completedDays.add(key);
        }

        int count = 0;

        Calendar cur = Calendar.getInstance();
        cur.set(Calendar.HOUR_OF_DAY, 0);
        cur.set(Calendar.MINUTE, 0);
        cur.set(Calendar.SECOND, 0);
        cur.set(Calendar.MILLISECOND, 0);

        while (true) {
            String key = cur.get(Calendar.YEAR) + "-" + cur.get(Calendar.DAY_OF_YEAR);
            if (completedDays.contains(key)) {
                count++;
                cur.add(Calendar.DAY_OF_YEAR, -1);
            } else break;
        }

        return count >= s.controllerStreakTarget;
    }

    private int countHighQuality(List<TechSession> techLogs) {
        int cnt = 0;
        for (TechSession t : techLogs) {
            if ("high-quality".equalsIgnoreCase(t.quality)) cnt++;
        }
        return cnt;
    }

    private boolean checkLowRescue(Settings s, List<MedicineLog> rescueLogs) {
        Calendar c = Calendar.getInstance();
        long now = System.currentTimeMillis();
        long THIRTY = 30L * 24 * 60 * 60 * 1000;

        Set<String> rescueDays = new HashSet<>();

        for (MedicineLog m : rescueLogs) {
            long t = m.getTimestamp().getTime();
            if (now - t <= THIRTY) {
                c.setTime(m.getTimestamp());
                String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
                rescueDays.add(key);
            }
        }

        return rescueDays.size() <= s.lowRescueThreshold;
    }


    // ============================================================
    // MODELS
    // ============================================================
    private static class TechSession {
        public String quality;
        public boolean completed;
        public Timestamp timestamp;

        public Date getDate() {
            return timestamp != null ? timestamp.toDate() : new Date(0);
        }
    }
}