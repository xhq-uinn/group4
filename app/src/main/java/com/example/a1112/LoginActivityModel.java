package com.example.a1112;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivityModel {
    //handles parent and provider login using firebase auth
    //handles child login using firestore database
    //and pass the result to presenter using callback

    private final FirebaseAuth auth;//auth, find parent/provider login
    private final FirebaseFirestore db;//fire database, find child login

    public interface LoginCallback {
        //interface call back to presenter
        //if login success, return user info & onboarding info to presenter
        void onSuccess(String id, boolean isChild, boolean needsOnboarding);
        //if fail, return error msg
        void onFailure(String message);
    }

    public LoginActivityModel() {
        //initialize auth/fire database
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void loginAdult(String email, String password, LoginCallback callback) {
        //auth login using email & password
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(task -> {
                    String uid = auth.getCurrentUser().getUid();

                    //get uid so as to get role
                    db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    callback.onFailure("User data missing");
                                    return;
                                }

                                //check onboarding
                                Boolean done = doc.getBoolean("hasCompletedOnboarding");
                                if (done == null) done = false;

                                if (!done) {
                                    //call back to presenter, go to onboadring
                                    callback.onSuccess("ADULT", false, true);   // <-- third param: onboarding
                                    return;
                                }

                                String role = doc.getString("role");
                                if (role == null) {
                                    callback.onFailure("Role missing");
                                    return;
                                }
                                //login success & onboarding yes, call back to presenter

                                callback.onSuccess(role, false, false);  // onboarding not needed
                            })
                            .addOnFailureListener(e ->
                                    callback.onFailure("Error loading role"));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Login failed"));
    }

    public void loginChild(String username, String password, LoginCallback callback) {
        //in the children collection, find matched username & password
        db.collection("children")
                .whereEqualTo("username", username)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(snap -> {

                    if (snap.isEmpty()) {
                        callback.onFailure("Wrong child username or password");
                        return;
                    }
                    //get childid so that can check onboarding
                    String childId = snap.getDocuments().get(0).getId();
                    Boolean done = snap.getDocuments().get(0).getBoolean("hasCompletedOnboarding");

                    boolean needsOnboarding = (done == null || !done);

                    callback.onSuccess(childId, true, needsOnboarding);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Error loading data"));
    }
}