package com.example.a1112;

import android.util.Log;

import com.google.firebase.firestore.*;
import com.google.firebase.Timestamp;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MotivationCalculator {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface Callback {
        void onComplete();
    }

    public void updateAllMotivation(String childId, Callback callback) {

        //get child's parentId
        db.collection("children").document(childId).get()
                .addOnSuccessListener(childDoc -> {
                    if (!childDoc.exists()) {
                        callback.onComplete();
                        return;
                    }

                    String parentId = childDoc.getString("parentId");
                    if (parentId == null) parentId = "";

                    // get parent motivation settings
                    loadSettings(parentId, settings ->
                            computeMotivation(childId, settings, callback));

                });
    }

//settings

    private static class Settings {
        //default
        int controllerStreakTarget = 7;
        int techniqueStreakTarget = 7;
        int techniqueHighQualityCount = 10;
        int lowRescueThreshold = 4;
    }

    private void loadSettings(String parentId, SettingsCallback cb) {

        Settings s = new Settings(); // defaults

        if (parentId == null || parentId.isEmpty()) {
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

                        Number c = (Number) doc.get("controllerStreakTarget");
                        if (c != null) s.controllerStreakTarget = c.intValue();

                        Number t = (Number) doc.get("techniqueStreakTarget");
                        if (t != null) s.techniqueStreakTarget = t.intValue();

                        Number h = (Number) doc.get("techniqueHighQualityCount");
                        if (h != null) s.techniqueHighQualityCount = h.intValue();

                        Number lr = (Number) doc.get("lowRescueThreshold");
                        if (lr != null) s.lowRescueThreshold = lr.intValue();
                    }

                    cb.onLoaded(s);

                })
                .addOnFailureListener(e -> {
                    Log.e("MOTIVATION", "Failed to load settings: " + e);
                    cb.onLoaded(s); // use defaults
                });
    }

    private interface SettingsCallback {
        void onLoaded(Settings s);
    }


    private void computeMotivation(String childId, Settings settings, Callback callback) {

        // 1.controller logs, technique sessions, rescue logs
        AtomicInteger loaded = new AtomicInteger(0);

        List<MedicineLog> controllerLogs = new ArrayList<>();
        List<MedicineLog> rescueLogs = new ArrayList<>();
        List<TechSession> techniqueLogs = new ArrayList<>();

        Runnable tryFinish = () -> {
            if (loaded.incrementAndGet() == 3) {
                finishCompute(childId, settings, controllerLogs, rescueLogs, techniqueLogs, callback);
            }
        };

        //Load controller logs
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "controller")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot d : q) {
                        MedicineLog m = d.toObject(MedicineLog.class);
                        controllerLogs.add(m);
                    }
                    tryFinish.run();
                });

        //Load rescue logs
        db.collection("medicineLogs")
                .whereEqualTo("childId", childId)
                .whereEqualTo("type", "rescue")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot d : q) {
                        MedicineLog m = d.toObject(MedicineLog.class);
                        rescueLogs.add(m);
                    }
                    tryFinish.run();
                });

        //Load technique logs
        db.collection("children")
                .document(childId)
                .collection("technique_sessions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    for (QueryDocumentSnapshot d : q) {
                        TechSession t = d.toObject(TechSession.class);
                        techniqueLogs.add(t);
                    }
                    tryFinish.run();
                });

    }

    // Model for technique session
    private static class TechSession {
        public String quality;
        public Timestamp timestamp;

        public Date getDate() {
            return (timestamp != null) ? timestamp.toDate() : new Date(0);
        }
    }


    private void finishCompute(
            String childId,
            Settings settings,
            List<MedicineLog> controllerLogs,
            List<MedicineLog> rescueLogs,
            List<TechSession> techLogs,
            Callback callback
    ) {

        // Compute streaks
        int controllerStreak = computeControllerStreak(controllerLogs);
        int techniqueStreak = computeTechniqueStreak(techLogs);

        // Compute badges
        List<String> badges = computeBadges(
                settings,
                controllerLogs,
                rescueLogs,
                techLogs
        );

        //Write to Firestore
        Map<String, Object> data = new HashMap<>();
        data.put("controllerStreak", controllerStreak);
        data.put("techniqueStreak", techniqueStreak);
        data.put("badges", badges);
        data.put("lastUpdated", Timestamp.now());

        db.collection("children")
                .document(childId)
                .collection("motivation")
                .document("status")
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> callback.onComplete());
    }


    // Streak Computation

    private int computeControllerStreak(List<MedicineLog> logs) {
        return computeDailyStreak(logs, log -> true);
    }

    private int computeTechniqueStreak(List<TechSession> logs) {

        // keep only High Quality
        List<TechSession> filtered = new ArrayList<>();
        for (TechSession t : logs) {
            if ("High Quality".equals(t.quality)) filtered.add(t);
        }

        // convert to fake 'log' with timestamp
        List<Date> dates = new ArrayList<>();
        for (TechSession t : filtered) {
            dates.add(t.getDate());
        }
        return computeDailyDateStreak(dates);
    }

    private int computeDailyStreak(List<MedicineLog> logs, LogFilter filter) {
        List<Date> dates = new ArrayList<>();
        for (MedicineLog m : logs) {
            if (filter.ok(m)) dates.add(m.getTimestamp());
        }
        return computeDailyDateStreak(dates);
    }

    private interface LogFilter {
        boolean ok(MedicineLog m);
    }

    private int computeDailyDateStreak(List<Date> dates) {
        if (dates.isEmpty()) return 0;

        // Normalize today
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayMillis = today.getTimeInMillis();

        // Collect unique days, ignoring future dates
        Set<String> uniqueDays = new HashSet<>();
        Calendar c = Calendar.getInstance();

        for (Date d : dates) {
            if (d.getTime() > todayMillis) {
                //ignore future logs caused by serverTimestamp delay
                continue;
            }
            c.setTime(d);
            uniqueDays.add(c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR));
        }

        int streak = 0;
        Calendar cursor = Calendar.getInstance();

        // Now count streak backward from today
        while (true) {
            String key = cursor.get(Calendar.YEAR) + "-" + cursor.get(Calendar.DAY_OF_YEAR);
            if (uniqueDays.contains(key)) {
                streak++;
                cursor.add(Calendar.DATE, -1);
            } else {
                break;
            }
        }

        return streak;
    }

    // Badge Computation

    private List<String> computeBadges(
            Settings settings,
            List<MedicineLog> controllerLogs,
            List<MedicineLog> rescueLogs,
            List<TechSession> techLogs
    ) {

        List<String> badges = new ArrayList<>();

        // 1. Perfect controller week (7 days)
        if (computeControllerStreak(controllerLogs) >= settings.controllerStreakTarget) {
            badges.add("PERFECT_CONTROLLER_WEEK");
        }

        // 2. 10 high quality technique sessions
        int hq = 0;
        for (TechSession t : techLogs) {
            if ("High Quality".equals(t.quality)) hq++;
        }
        if (hq >= settings.techniqueHighQualityCount) {
            badges.add("TEN_HIGH_QUALITY_TECHNIQUE");
        }

        // 3. Low rescue month (<= threshold in 30 days)
        // Low-rescue month: count rescue "days", not rescue "doses"
        Set<String> rescueDays = new HashSet<>();

        Date now = new Date();
        long THIRTY = 30L * 24 * 60 * 60 * 1000;

        Calendar c = Calendar.getInstance();

        for (MedicineLog m : rescueLogs) {
            Date d = m.getTimestamp();

            // skip future timestamps (possible clock mismatch)
            if (d.getTime() > now.getTime()) continue;

            // only count last 30 days
            if (now.getTime() - d.getTime() <= THIRTY) {
                c.setTime(d);
                String key = c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
                rescueDays.add(key);  // ensure 1 day = 1 count
            }
        }

        int rescueDayCount = rescueDays.size();

        if (rescueDayCount <= settings.lowRescueThreshold) {
            badges.add("LOW_RESCUE_MONTH");
        }

        return badges;
    }

}