package com.example.a1112;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivityView extends AppCompatActivity {
    //view display UI, pass button clicks to the presenter, and provide methods for presenter to call

    private LoginActivityPresenter presenter;

    private EditText emailField;
    private EditText adultPasswordField;
    private EditText childUsernameField;
    private EditText childPasswordField;
    private Button loginButton;
    private TextView signupButton;
    private TextView forgotPasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        presenter = new LoginActivityPresenter(this);

        emailField = findViewById(R.id.emailField);
        adultPasswordField = findViewById(R.id.adultPasswordField);
        childUsernameField = findViewById(R.id.childUsernameField);
        childPasswordField = findViewById(R.id.childPasswordField);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupLink);
        forgotPasswordButton = findViewById(R.id.forgotPasswordText);

        //get user input & pass it to presenter
        loginButton.setOnClickListener(v -> presenter.handleLogin(
                emailField.getText().toString().trim(),
                adultPasswordField.getText().toString().trim(),
                childUsernameField.getText().toString().trim(),
                childPasswordField.getText().toString().trim()
        ));

        //click signup-> navigate to signup activity
        signupButton.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        //similar
        forgotPasswordButton.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
    }



    public void showMessage(String msg) {
        //show error msg if login fail
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void navigateToParentHome() {
        startActivity(new Intent(this, ParentHomeActivity.class));
        finish();
    }

    public void navigateToProviderHome() {
        startActivity(new Intent(this, ProviderHomeActivity.class));
        finish();
    }

    public void navigateToChildHome(String childId) {
        Intent i = new Intent(this, ChildHomeActivity.class);
        i.putExtra("childId", childId); //pass childid
        startActivity(i);
        finish();
    }

    public void navigateToOnboarding(String childId) {
        //if adult login, childId = null
        //if child login, childId = chilid in the fire database
        Intent i = new Intent(this, OnboardingActivity.class);
        if (childId != null) {
            i.putExtra("childId", childId);
        }
        startActivity(i);
        finish();
    }
}