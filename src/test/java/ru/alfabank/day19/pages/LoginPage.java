package ru.alfabank.day19.pages;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

public class LoginPage {

    public void assertComponentsVisible() {
        $("h2").shouldHave(text("Login Page"));
        $("div.large-12.columns a").shouldHave(text("Elemental Selenium"));
    }

    public void setUserName(String userName) {
        $("#username").setValue(userName);
    }

    public void setPassword(String password) {
        $("#password").setValue(password);
    }

    public void clickLoginButton() {
        $("button[type='submit']").click();
    }

}
