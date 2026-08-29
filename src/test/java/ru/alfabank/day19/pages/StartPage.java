package ru.alfabank.day19.pages;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class StartPage {

    public void openPage() {
        open("https://the-internet.herokuapp.com/");
    }

    public void clickFormAuthenticationLink() {
        $(byText("Form Authentication")).click();
    }
}
