package ru.alfabank.day19.pages;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

public class SecurePage {

    public void assertFlashHasText(String expectedText) {
        $(".flash.success").shouldHave(text(expectedText));
    }

    public void assertLogoutButtonVisible() {
        $(byText("Logout")).shouldBe(visible);
    }

    public void clickLogout() {
        $(byText("Logout")).click();
    }
}
