package com.example.a1112;

import java.util.List;

public class Child {
    private String id;  // Firestore document ID
    private String name;
    private int age;
    private String note;
    private String username;
    private String parentId;
    private boolean hasCompletedOnboarding;
    private List<String> sharedProviders;

    public Child() {
        // Firestore needs empty constructor
    }

    public Child(String id, String name, int age, String note,
                 String username, String parentId,
                 boolean hasCompletedOnboarding, List<String> sharedProviders) {

        this.id = id;
        this.name = name;
        this.age = age;
        this.note = note;
        this.username = username;
        this.parentId = parentId;
        this.hasCompletedOnboarding = hasCompletedOnboarding;
        this.sharedProviders = sharedProviders;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getNote() { return note; }
    public String getUsername() { return username; }
    public String getParentId() { return parentId; }
    public boolean isHasCompletedOnboarding() { return hasCompletedOnboarding; }
    public List<String> getSharedProviders() { return sharedProviders; }


    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setNote(String note) { this.note = note; }
    public void setUsername(String username) { this.username = username; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setHasCompletedOnboarding(boolean hasCompletedOnboarding) { this.hasCompletedOnboarding = hasCompletedOnboarding; }
    public void setSharedProviders(List<String> sharedProviders) { this.sharedProviders = sharedProviders; }
}
