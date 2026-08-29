package ru.alfabank.day19.tests;

import org.junit.jupiter.api.Test;
import ru.alfabank.day19.pages.*;

public class LoginTest {

    @Test
    public void successfulLoginTest() {
        String userName = "tomsmith";
        String password = "SuperSecretPassword!";

        StartPage startPage = new StartPage();
        startPage.openPage();
        startPage.clickFormAuthenticationLink();

        LoginPage loginPage = new LoginPage();
        loginPage.assertComponentsVisible();

        loginPage.setUserName(userName);
        loginPage.setPassword(password);
        loginPage.clickLoginButton();

        SecurePage securePage = new SecurePage();
        securePage.assertFlashHasText("You logged into a secure area!");
        securePage.assertLogoutButtonVisible();

        securePage.clickLogout();

        LoginPage loginPageAfterLogout = new LoginPage();
        loginPageAfterLogout.assertComponentsVisible();
    }

    @Test
    public void invalidLoginTest() { 
        String userName = "admin";
        String password = "1234";

        StartPage startPage = new StartPage();
        startPage.openPage();
        startPage.clickFormAuthenticationLink();

        LoginPage loginPage = new LoginPage();
        loginPage.assertComponentsVisible();
        loginPage.setUserName(userName);
        loginPage.setPassword(password);
        loginPage.clickLoginButton();

        ErrorLoginPage errorLoginPage = new ErrorLoginPage();
        errorLoginPage.assertFlashHasText();
    }
}
