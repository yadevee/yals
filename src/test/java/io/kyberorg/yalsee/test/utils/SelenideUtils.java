package io.kyberorg.yalsee.test.utils;

import java.time.Duration;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.title;

/**
 * Useful utils for Selenide tests.
 *
 * @since 2.7.4
 */
public final class SelenideUtils {
    private SelenideUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Waits until Site Loads (Body tag appears).
     *
     * @param durationInSeconds wait duration in seconds.
     */
    public static void waitUntilSiteLoads(final int durationInSeconds) {
        $("body").shouldBe(visible, Duration.ofSeconds(durationInSeconds));
    }

    /**
     * Just more readable alias for Selenide's {@link com.codeborne.selenide.Selenide#title()}.
     *
     * @return string with title of opened page
     */
    public static String getPageTitle() {
        return title();
    }
}
