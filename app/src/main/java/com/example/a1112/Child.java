package com.example.a1112;

public class Child {
    private String id;  // Firestore 文档 ID
    private String name;
    private String dob;
    private String note;

    public Child() {
        // Firestore 需要空构造函数
    }

    public Child(String id, String name, String dob, String note) {
        this.id = id;
        this.name = name;
        this.dob = dob;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDob() {
        return dob;
    }

    public String getNote() {
        return note;
    }
}
