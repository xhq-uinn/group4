package com.example.a1112;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoginActivityPresenter.
 * Uses Mockito to mock the View and Model, and tries to cover all branches.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoginActivityPresenterTest {

    @Mock
    private LoginActivityView mockView;

    @Mock
    private LoginActivityModel mockModel;

    @Captor
    private ArgumentCaptor<LoginActivityModel.LoginCallback> callbackCaptor;

    private LoginActivityPresenter presenter;

    @Before
    public void setUp() {
        presenter = new LoginActivityPresenter(mockView, mockModel);
    }



    @Test
    public void adultAndChildBothFilled_showsMixedLoginError_fromAdultBranch() {
        presenter.handleLogin("p@example.com", "pass", "kidUser", "kidPass");

        verify(mockView).showMessage("child login or adult login, not both");
        verifyNoInteractions(mockModel);
    }

    @Test
    public void adultAndChildBothFilled_showsMixedLoginError_fromChildBranch() {
        presenter.handleLogin("p@example.com", "", "kidUser", "kidPass");

        verify(mockView).showMessage("child login or adult login, not both");
        verifyNoInteractions(mockModel);
    }


    @Test
    public void adultLogin_needsOnboarding_navigatesToOnboarding() {
        presenter.handleLogin("p@example.com", "pass", "", "");

        verify(mockModel).loginAdult(eq("p@example.com"), eq("pass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onSuccess("ADULT", false, true);

        verify(mockView).navigateToOnboarding(null);
        verify(mockView, never()).navigateToParentHome();
        verify(mockView, never()).navigateToProviderHome();
    }

    @Test
    public void adultLogin_parentRole_navigatesToParentHome() {
        presenter.handleLogin("p@example.com", "pass", "", "");

        verify(mockModel).loginAdult(eq("p@example.com"), eq("pass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onSuccess("Parent", false, false);

        verify(mockView).navigateToParentHome();
        verify(mockView, never()).navigateToProviderHome();
        verify(mockView, never()).navigateToOnboarding(any());
    }

    @Test
    public void adultLogin_providerRole_navigatesToProviderHome() {
        presenter.handleLogin("p@example.com", "pass", "", "");

        verify(mockModel).loginAdult(eq("p@example.com"), eq("pass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onSuccess("Provider", false, false);

        verify(mockView).navigateToProviderHome();
        verify(mockView, never()).navigateToParentHome();
        verify(mockView, never()).navigateToOnboarding(any());
    }

    @Test
    public void adultLogin_invalidRole_showsInvalidRoleMessage() {
        presenter.handleLogin("p@example.com", "pass", "", "");

        verify(mockModel).loginAdult(eq("p@example.com"), eq("pass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onSuccess("SomeOtherRole", false, false);

        verify(mockView).showMessage("Invalid role");
        verify(mockView, never()).navigateToParentHome();
        verify(mockView, never()).navigateToProviderHome();
        verify(mockView, never()).navigateToOnboarding(any());
    }

    @Test
    public void adultLogin_failure_showsErrorMessage() {
        presenter.handleLogin("p@example.com", "pass", "", "");

        verify(mockModel).loginAdult(eq("p@example.com"), eq("pass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onFailure("Login failed");

        verify(mockView).showMessage("Login failed");
    }


    @Test
    public void childLogin_needsOnboarding_navigatesToOnboardingWithChildId() {
        presenter.handleLogin("", "", "kidUser", "kidPass");

        verify(mockModel).loginChild(eq("kidUser"), eq("kidPass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onSuccess("child123", true, true);

        verify(mockView).navigateToOnboarding("child123");
        verify(mockView, never()).navigateToChildHome(anyString());
    }

    @Test
    public void childLogin_noOnboarding_navigatesToChildHome() {
        presenter.handleLogin("", "", "kidUser", "kidPass");

        verify(mockModel).loginChild(eq("kidUser"), eq("kidPass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onSuccess("child123", true, false);

        verify(mockView).navigateToChildHome("child123");
        verify(mockView, never()).navigateToOnboarding(any());
    }

    @Test
    public void childLogin_failure_showsErrorMessage() {
        presenter.handleLogin("", "", "kidUser", "kidPass");

        verify(mockModel).loginChild(eq("kidUser"), eq("kidPass"), callbackCaptor.capture());
        LoginActivityModel.LoginCallback cb = callbackCaptor.getValue();

        cb.onFailure("Wrong child username or password");

        verify(mockView).showMessage("Wrong child username or password");
    }


    @Test
    public void incompleteAdultCredentials_showsFillInLoginInfo() {
        presenter.handleLogin("p@example.com", "", "", "");

        verify(mockView).showMessage("Fill in login info");
        verifyNoInteractions(mockModel);
    }

    @Test
    public void incompleteChildCredentials_showsFillInLoginInfo() {
        presenter.handleLogin("", "", "kidUser", "");

        verify(mockView).showMessage("Fill in login info");
        verifyNoInteractions(mockModel);
    }

    @Test
    public void allEmpty_showsFillInLoginInfo() {
        presenter.handleLogin("", "", "", "");

        verify(mockView).showMessage("Fill in login info");
        verifyNoInteractions(mockModel);
    }
}

