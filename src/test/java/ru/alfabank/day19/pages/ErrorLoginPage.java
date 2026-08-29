package ru.alfabank.day19.pages;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class ErrorLoginPage {

    public void assertFlashHasText() {
        $(".flash.error").shouldHave(text("Your username is invalid!"));
    }
}
