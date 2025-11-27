package com.example.a1112;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Date;
import java.util.Map;


@IgnoreExtraProperties
public class MedicineLog {

    //exclude id because its not stored as a field in firestore database
    @Exclude
    private String id;

    private String childId;
    private String medicineId;
    private String medicineName;
    private String type;
    private int doseCount;
    private Date timestamp;
    private String loggedBy;
    private String postFeeling;

    public MedicineLog() {}
    //same as before
    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getDoseCount() { return doseCount; }
    public void setDoseCount(int doseCount) { this.doseCount = doseCount; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getLoggedBy() { return loggedBy; }
    public void setLoggedBy(String loggedBy) { this.loggedBy = loggedBy; }

    public String getPostFeeling() { return postFeeling; }
    public void setPostFeeling(String postFeeling) { this.postFeeling = postFeeling; }
}