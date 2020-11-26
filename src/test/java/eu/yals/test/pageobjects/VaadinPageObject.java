package eu.yals.test.pageobjects;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.disappears;
import static com.codeborne.selenide.Selenide.$;

/**
 * Common Vaadin elements and methods.
 *
 * @since 2.7.4
 */
public class VaadinPageObject {
    public static final SelenideElement LOADING_BAR = $(".v-loading-indicator");

    /**
     * Ensures that site is loaded and Vaadin loading bar already disappear.
     */
    public static void waitForVaadin() {
        $(LOADING_BAR).waitUntil(disappears, Configuration.timeout);
    }
}