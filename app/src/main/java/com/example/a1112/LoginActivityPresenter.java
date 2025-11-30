package com.example.a1112;

public class LoginActivityPresenter {
    //presenter decide whether it's (child login) or (parent/provider login)
    //(we call "parent/provider login" as "adult login" below)
    //call model for auth login or fire database login
    //update UI (through view), according to callback result from model

    private final LoginActivityModel model;
    private final LoginActivityView view;

    public LoginActivityPresenter(LoginActivityView view) {
        this.view = view;
        this.model = new LoginActivityModel();
    }

    public LoginActivityPresenter(LoginActivityView view, LoginActivityModel model) {
        this.view = view;
        this.model = model;
    }

    public void handleLogin(
            String email, //parent/provider email
            String adultPassword, //parent/provider password
            String childUsername,
            String childPassword
    ) {
        //adult login
        if (!email.isEmpty() && !adultPassword.isEmpty()) {

            //child section should be empty to prevent mixed login
            if (!childUsername.isEmpty() || !childPassword.isEmpty()) {
                view.showMessage("child login or adult login, not both");
                return;
            }

            //call model for adult login
            model.loginAdult(email, adultPassword, new LoginActivityModel.LoginCallback() {
                @Override
                public void onSuccess(String id, boolean isChild, boolean needsOnboarding) {
                    if (needsOnboarding) {

                        //onboarding no-> call view to navigate to onboarding
                        view.navigateToOnboarding(null);
                        return;
                    }

                    //onboarding yes-> call view to navigate to their respective homepage
                    if (id.equals("Parent")) {
                        view.navigateToParentHome();
                    } else if (id.equals("Provider")) {
                        view.navigateToProviderHome();
                    } else {
                        view.showMessage("Invalid role");
                    }
                }

                @Override
                public void onFailure(String message) {
                    //if fail, show error msg through view
                    view.showMessage(message);
                }
            });

            return;
        }

        //child login
        if (!childUsername.isEmpty() && !childPassword.isEmpty()) {

            //so adult section should be empty
            if (!email.isEmpty() || !adultPassword.isEmpty()) {
                view.showMessage("child login or adult login, not both");
                return;
            }

            //call model for child login
            model.loginChild(childUsername, childPassword,
                    new LoginActivityModel.LoginCallback() {

                        //similar to the previous
                        @Override
                        public void onSuccess(String childId, boolean isChild, boolean needsOnboarding) {
                            if (needsOnboarding) {
                                view.navigateToOnboarding(childId);
                            } else {
                                view.navigateToChildHome(childId);
                            }
                        }

                        @Override
                        public void onFailure(String message) {
                            view.showMessage(message);
                        }
                    }
            );

            return;
        }

        //if login info entrance not complete
        view.showMessage("Fill in login info");
    }
}