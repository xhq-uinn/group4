package com.example.a1112;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ControllerSchedule {
    @Exclude
    private String id;

    private String childId;
    private int mondayDoses;
    private int tuesdayDoses;
    private int wednesdayDoses;
    private int thursdayDoses;
    private int fridayDoses;
    private int saturdayDoses;
    private int sundayDoses;

    public ControllerSchedule() {}

    public ControllerSchedule(String childId) {
        this.childId = childId;
        this.mondayDoses = 0;
        this.tuesdayDoses = 0;
        this.wednesdayDoses = 0;
        this.thursdayDoses = 0;
        this.fridayDoses = 0;
        this.saturdayDoses = 0;
        this.sundayDoses = 0;
    }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public int getMondayDoses() { return mondayDoses; }
    public void setMondayDoses(int mondayDoses) { this.mondayDoses = mondayDoses; }

    public int getTuesdayDoses() { return tuesdayDoses; }
    public void setTuesdayDoses(int tuesdayDoses) { this.tuesdayDoses = tuesdayDoses; }

    public int getWednesdayDoses() { return wednesdayDoses; }
    public void setWednesdayDoses(int wednesdayDoses) { this.wednesdayDoses = wednesdayDoses; }

    public int getThursdayDoses() { return thursdayDoses; }
    public void setThursdayDoses(int thursdayDoses) { this.thursdayDoses = thursdayDoses; }

    public int getFridayDoses() { return fridayDoses; }
    public void setFridayDoses(int fridayDoses) { this.fridayDoses = fridayDoses; }

    public int getSaturdayDoses() { return saturdayDoses; }
    public void setSaturdayDoses(int saturdayDoses) { this.saturdayDoses = saturdayDoses; }

    public int getSundayDoses() { return sundayDoses; }
    public void setSundayDoses(int sundayDoses) { this.sundayDoses = sundayDoses; }

    public int getDoseForDay(int dayOfWeek) {
        switch (dayOfWeek) {
            case 1: return sundayDoses;
            case 2: return mondayDoses;
            case 3: return tuesdayDoses;
            case 4: return wednesdayDoses;
            case 5: return thursdayDoses;
            case 6: return fridayDoses;
            case 7: return saturdayDoses;
            default: return 0;
        }
    }
}
