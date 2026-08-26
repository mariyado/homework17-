package ru.alfabank.day19;

import com.codeborne.selenide.Configuration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Condition.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LoginTest {

    @Test
    public void successfulLoginTest() {
        open("https://the-internet.herokuapp.com/");
        $(byText("Form Authentication")).click();
        $("h2").shouldHave(text("Login Page"));
        $("#username").setValue("tomsmith");
        $("#password").setValue("SuperSecretPassword!");
        $("button[type='submit']").click();
        $(".flash.success").shouldHave(text("You logged into a secure area!"));
        $(byText("Logout")).shouldBe(visible);
        $(byText("Logout")).click();
        $("h2").shouldHave(text("Login Page"));
    }

    @Test
    public void invalidLoginTest() {
        open("https://the-internet.herokuapp.com/");
        $(byText("Form Authentication")).click();
        $("div.large-12.columns a").shouldHave(text("Elemental Selenium"));
        $("#username").setValue("admin");
        $("#password").setValue("1234");
        $("button[type='submit']").click();
        $(".flash.error").shouldHave(text("Your username is invalid!"));
    }
}
