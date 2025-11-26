package com.example.a1112;

import java.util.Date;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.Exclude;

//annotation to tell firebase ignore what it doesnt recognize
@IgnoreExtraProperties

public class Medicine {
    //tells firebase not to store id as a field becuase our medicine model's id is the firebase instance's doc id
    @Exclude
    private String id;

    private String childId;
    private String name;
    private String type;
    private String unitType;
    private int totalAmount;
    private int currentAmount;
    private Date purchaseDate;
    private Date expiryDate;
    private String lastUpdatedBy; // "parent" or "child"
    private Date lastUpdatedAt;
    private Date createdAt;
    private boolean flaggedLowByChild;
    private Date flaggedAt;

    //empty constructor that firestore needs
    public Medicine() {}

    //constructor with fields
    public Medicine(String childId, String name, String type, String unitType,
                    Date purchaseDate, Date expiryDate, int totalAmount) {
        this.childId = childId;
        this.name = name;
        this.type = type;
        this.unitType = unitType;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.totalAmount = totalAmount;
        this.currentAmount = totalAmount; //default current amount to total because it can be updated
        this.lastUpdatedBy = "parent"; //also default last updated to parent because new inventory instances are always parent
        this.lastUpdatedAt = new Date();
        this.createdAt = new Date();
        this.flaggedLowByChild = false; // default to false because it cant be flagged before being part of inventory
        this.flaggedAt = null;
    }

    //exclude getId and setId because there is no field id in firestore we just use document id
    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
    }

    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public Date getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Date lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isFlaggedLowByChild() { return flaggedLowByChild; }
    public void setFlaggedLowByChild(boolean flagged) { this.flaggedLowByChild = flagged; }

    public Date getFlaggedAt() { return flaggedAt; }
    public void setFlaggedAt(Date flaggedAt) { this.flaggedAt = flaggedAt; }

   // useful methods, also excluded in firestore
    @Exclude
    //calculate percentage of total amount left
    public int getPercentageLeft() {
        if (totalAmount > 0) {
            return (currentAmount * 100) / totalAmount;
        }
        return 0;
    }

    //check if a medicine is running low or child flagged it low
    @Exclude
    public boolean isLow() {
        return getPercentageLeft() <= 20 || isFlaggedLowByChild();
    }

    //check for expiry
    @Exclude
    public boolean isExpired() {
        return new Date().after(expiryDate);
    }

    //overriding default toString method to output a meaningful representation
    @Override
    public String toString() {
        return name + " - " + currentAmount + "/" + totalAmount + " (" + getPercentageLeft() + "%)";
    }
}