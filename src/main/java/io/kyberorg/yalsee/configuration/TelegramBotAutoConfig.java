package io.kyberorg.yalsee.configuration;

import io.kyberorg.yalsee.constants.App;
import io.kyberorg.yalsee.telegram.TelegramBot;
import io.kyberorg.yalsee.utils.AppUtils;
import io.kyberorg.yalsee.utils.UrlExtraValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.kyberorg.yalsee.utils.UrlExtraValidator.VALID;

/**
 * Registering bot at start time.
 *
 * @since 2.4
 */
@Slf4j
@ConditionalOnBean(TelegramBot.class)
@Configuration
public class TelegramBotAutoConfig {
    private static final String TAG = "[" + TelegramBotAutoConfig.class.getSimpleName() + "]";

    private final List<BotSession> sessions = new ArrayList<>();

    private final TelegramBot telegramBot;
    private final AppUtils appUtils;

    /**
     * Constructor for Spring autowiring.
     *
     * @param bot   Telegram bot, which handles connections
     * @param utils app utils for getting server url and defining if telegram integration is enabled
     */
    public TelegramBotAutoConfig(final TelegramBot bot, final AppUtils utils) {
        this.telegramBot = bot;
        this.appUtils = utils;
    }

    /**
     * Starts interaction with Telegram after {@link TelegramBot} component is ready.
     */
    @PostConstruct
    public void start() {
        if (appUtils.isTelegramDisabled()) {
            log.info("{} Telegram bot is disabled. Skipping configuration...", TAG);
            return;
        }

        log.info("{} Registering telegram bot", TAG);

        boolean isBotAvailable = Objects.nonNull(telegramBot) && isServerUrlAvailable();
        String botStatus = isBotAvailable ? "available" : "not available";
        log.info("{} Bot Status: {}", TAG, botStatus);

        if (isBotAvailable) {
            try {
                TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
                log.debug("{} Bot token: {}", TAG, telegramBot.getBotToken());
                sessions.add(api.registerBot(telegramBot));
            } catch (TelegramApiException e) {
                log.error("{} Failed to register bot", TAG);
                log.debug("", e);
            }
        }
    }

    /**
     * Destroys all telegram sessions before application stops.
     */
    @PreDestroy
    public void stop() {
        sessions.forEach(session -> {
            if (session != null) {
                session.stop();
            }
        });
    }

    private boolean isServerUrlAvailable() {
        String serverHostname = appUtils.getServerUrl();
        boolean isServerUrlPresentAndValid = UrlExtraValidator.isUrlValid(serverHostname).equals(VALID);
        if (isServerUrlPresentAndValid) {
            return true;
        } else {
            log.error("{} Server URL is not valid or missing. Did '{}' property or ENV '{}' was set?",
                    TAG, App.Properties.SERVER_URL, App.Env.SERVER_URL);
            log.info("{} Server URL is {}", TAG, serverHostname);
            return false;
        }
    }
}
