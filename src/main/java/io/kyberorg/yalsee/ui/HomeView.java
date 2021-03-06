package io.kyberorg.yalsee.ui;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import io.kyberorg.yalsee.Endpoint;
import io.kyberorg.yalsee.constants.App;
import io.kyberorg.yalsee.exception.error.YalseeErrorBuilder;
import io.kyberorg.yalsee.json.StoreRequestJson;
import io.kyberorg.yalsee.services.overall.OverallService;
import io.kyberorg.yalsee.utils.AppUtils;
import io.kyberorg.yalsee.utils.ErrorUtils;
import io.kyberorg.yalsee.utils.push.Broadcaster;
import io.kyberorg.yalsee.utils.push.Push;
import io.kyberorg.yalsee.utils.push.PushCommand;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.olli.ClipboardHelper;

import static io.kyberorg.yalsee.constants.HttpCode.STATUS_200;
import static io.kyberorg.yalsee.constants.HttpCode.STATUS_201;
import static io.kyberorg.yalsee.utils.push.PushCommand.UPDATE_COUNTER;

@Slf4j
@SpringComponent
@UIScope
@CssImport("./css/common_styles.css")
@CssImport("./css/home_view.css")
@Route(value = Endpoint.UI.HOME_PAGE, layout = MainView.class)
@PageTitle("Yalsee - the link shortener")
public class HomeView extends HorizontalLayout {
    private static final String TAG = "[" + HomeView.class.getSimpleName() + "]";

    private final Div leftDiv = new Div();
    private final VerticalLayout centralLayout = new VerticalLayout();
    private final Div rightDiv = new Div();

    private final Component mainArea = mainArea();
    private final Component overallArea = overallArea();
    private final Component resultArea = resultArea();
    private final Component qrCodeArea = qrCodeArea();

    private final OverallService overallService;
    private final AppUtils appUtils;
    private final ErrorUtils errorUtils;
    private Registration broadcasterRegistration;

    private Span titleLongPart;
    private TextField input;
    private Button submitButton;
    private Anchor shortLink;
    private ClipboardHelper clipboardHelper;
    private Image qrCode;

    private Span linkCounter;

    private Notification errorNotification;

    /**
     * Create {@link HomeView}.
     *
     * @param overallService overall service for getting number of links
     * @param appUtils       application utils for getting server location and API location
     * @param errorUtils     error utils to report to bugsnag
     */
    public HomeView(
            final OverallService overallService, final AppUtils appUtils, final ErrorUtils errorUtils) {
        this.overallService = overallService;
        this.appUtils = appUtils;
        this.errorUtils = errorUtils;

        init();
        applyStyle();
        applyLoadState();
    }

    private void init() {
        this.setId(IDs.VIEW_ID);

        add(leftDiv, centralLayout, rightDiv);
        centralLayout.add(mainArea, overallArea, resultArea, qrCodeArea);
    }

    private void applyStyle() {
        leftDiv.addClassName("responsive-div");
        centralLayout.addClassName("responsive-center");
        rightDiv.addClassName("responsive-div");

        titleLongPart.addClassName("title-long-text");
    }

    private void applyLoadState() {
        long linksStored = overallService.numberOfStoredLinks();
        linkCounter.setText(Long.toString(linksStored));

        input.setAutofocus(true);
        submitButton.setEnabled(true);

        mainArea.setVisible(true);
        overallArea.setVisible(true);
        resultArea.setVisible(false);
        qrCodeArea.setVisible(false);
    }

    private VerticalLayout mainArea() {
        Span titlePartOne = new Span("Make your ");
        titleLongPart = new Span("long ");
        Span titleLastPart = new Span("links short");

        H2 title = new H2(titlePartOne, titleLongPart, titleLastPart);
        title.setId(IDs.TITLE);
        title.addClassName("compact-title");

        input = new TextField("Your very long URL here:");
        input.setId(IDs.INPUT);
        input.setPlaceholder("https://mysuperlongurlhere.tld");
        input.setWidthFull();

        Span publicAccessBanner =
                new Span("Note: all links considered as public and can be used by anyone");
        publicAccessBanner.setId(IDs.BANNER);

        submitButton = new Button("Shorten it!");
        submitButton.setId(IDs.SUBMIT_BUTTON);
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClickListener(this::onSaveLink);

        VerticalLayout mainArea =
                new VerticalLayout(title, input, publicAccessBanner, submitButton);
        mainArea.setId(IDs.MAIN_AREA);
        mainArea.addClassNames("main-area", "border");
        return mainArea;
    }

    private HorizontalLayout overallArea() {
        Span overallTextStart = new Span("Yalsee already saved ");

        linkCounter = new Span();
        linkCounter.setId(IDs.OVERALL_LINKS_NUMBER);
        Span overallTextEnd = new Span(" links");

        Span overallText = new Span(overallTextStart, linkCounter, overallTextEnd);
        overallText.setId(IDs.OVERALL_LINKS_TEXT);

        HorizontalLayout overallArea = new HorizontalLayout(overallText);
        overallArea.setId(IDs.OVERALL_AREA);
        overallArea.addClassNames("overall-area", "border", "joint-area");
        overallArea.setWidthFull();
        return overallArea;
    }

    private HorizontalLayout resultArea() {
        HorizontalLayout resultArea = new HorizontalLayout();
        resultArea.setId(IDs.RESULT_AREA);

        Span emptySpan = new Span();

        shortLink = new Anchor("", "");
        shortLink.setId(IDs.SHORT_LINK);
        shortLink.addClassName("strong-link");

        com.vaadin.flow.component.icon.Icon copyLinkImage;
        copyLinkImage = new com.vaadin.flow.component.icon.Icon(VaadinIcon.COPY);
        copyLinkImage.addClickListener(this::copyLinkToClipboard);

        clipboardHelper = new ClipboardHelper();
        clipboardHelper.wrap(copyLinkImage);
        clipboardHelper.setId(IDs.COPY_LINK_BUTTON);

        resultArea.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        resultArea.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        resultArea.add(emptySpan, shortLink, clipboardHelper);
        resultArea.addClassNames("result-area", "border");
        resultArea.setWidthFull();
        return resultArea;
    }

    private Div qrCodeArea() {
        Div qrCodeArea = new Div();
        qrCodeArea.setId(IDs.QR_CODE_AREA);

        qrCode = new Image();
        qrCode.setId(IDs.QR_CODE);
        qrCode.setSrc("");
        qrCode.setAlt("qrCode");

        qrCodeArea.add(qrCode);
        qrCodeArea.addClassNames("qr-area", "border", "joint-area");
        qrCodeArea.setWidthFull();
        return qrCodeArea;
    }

    private Notification getLinkCopiedNotification() {
        final int notificationDuration = 3000;
        Notification notification =
                new Notification("Short link copied", notificationDuration, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        return notification;
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> ui.access(() -> {
            log.trace("{} Push received. {} ID: {}, Message: {}",
                    TAG, HomeView.class.getSimpleName(), ui.getUIId(), message);
            Push push = Push.fromMessage(message);
            if (push.valid()) {
                PushCommand command = push.getPushCommand();
                if (command == UPDATE_COUNTER) {
                    updateCounter();
                } else {
                    log.warn("{} got unknown push command: '{}'", TAG, push.getPushCommand());
                }
            } else {
                log.debug("{} not valid push command: '{}'", TAG, message);
            }
        }));
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        // Cleanup
        log.trace("{} {} {} detached", TAG, HomeView.class.getSimpleName(), detachEvent.getUI().getUIId());
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void onSaveLink(final ClickEvent<Button> buttonClickEvent) {
        log.trace("{} Submit button clicked. By client? {}", TAG, buttonClickEvent.isFromClient());

        cleanErrors();
        cleanResults();

        boolean isFormValid = true;
        String longUrl = input.getValue();
        log.debug("{} Got long URL: {}", TAG, longUrl);
        cleanForm();

        if (StringUtils.isBlank(longUrl)) {
            String errorMessage = "Long URL cannot be empty";
            showError(errorMessage);
            isFormValid = false;
        } else {
            try {
                longUrl = AppUtils.makeFullUri(longUrl).toString();
            } catch (RuntimeException e) {
                log.error("{} URL validation failed", TAG);
                log.debug("", e);
                showError("Got malformed URL or not URL at all");
                isFormValid = false;
            }
        }

        if (isFormValid) {
            cleanResults();
            sendLink(longUrl);
        } else {
            log.debug("{} Form is not valid", TAG);
        }
    }

    private void copyLinkToClipboard(
            final ClickEvent<com.vaadin.flow.component.icon.Icon> buttonClickEvent) {
        log.trace("{} Copy link button clicked. From client? {}", TAG, buttonClickEvent.isFromClient());
        getLinkCopiedNotification().open();
        //All other actions are performed by component wrapper
    }

    private void sendLink(final String link) {
        final String apiRoute = Endpoint.Api.STORE_API;
        StoreRequestJson json = StoreRequestJson.create().withLink(link);
        HttpResponse<JsonNode> response =
                Unirest.post(appUtils.getAPIHostPort() + apiRoute).body(json).asJson();
        log.debug("{} Got reply from Store API. Status: {}, Body: {}",
                TAG, response.getStatus(), response.getBody().toPrettyString());
        if (response.isSuccess()) {
            onSuccessStoreLink(response);
        } else {
            onFailStoreLink(response);
        }
    }

    private void onSuccessStoreLink(final HttpResponse<JsonNode> response) {
        cleanErrors();
        cleanForm();
        if (response.getStatus() == STATUS_201) {
            JsonNode json = response.getBody();
            String ident = json.getObject().getString("ident");
            log.debug("{} Got reply with ident: {}", TAG, ident);
            if (StringUtils.isNotBlank(ident)) {
                shortLink.setText(appUtils.getShortUrl() + "/" + ident);
                shortLink.setHref(appUtils.getShortUrl() + "/" + ident);
                resultArea.setVisible(true);
                clipboardHelper.setContent(shortLink.getText());
                Broadcaster.broadcast(Push.command(UPDATE_COUNTER).toString());
                generateQRCode(ident);
            } else {
                showError("Internal error. Got malformed reply from server");
                errorUtils.reportToBugsnag(YalseeErrorBuilder
                        .withTechMessage(String.format("onSuccessStoreLink: Malformed JSON: %s",
                                response.getBody().toPrettyString()
                        ))
                        .withStatus(response.getStatus())
                        .build());
            }
        } else {
            log.error("{} Got false positive. Status: {}, Body: {}",
                    TAG, response.getStatus(), response.getBody().toPrettyString());

            showError("Something wrong was happened at server-side. Issue already reported");
            errorUtils.reportToBugsnag(YalseeErrorBuilder
                    .withTechMessage(String.format("onSuccessStoreLink: Got false positive. Body: %s",
                            response.getBody().toPrettyString()
                    ))
                    .withStatus(response.getStatus()).build());
        }
    }

    private void onFailStoreLink(final HttpResponse<JsonNode> response) {
        JsonNode json = response.getBody();
        log.error("{} Failed to store link. Reply: {}", TAG, json);
        String message;
        try {
            message = json.getObject().getString("message");
        } catch (JSONException e) {
            log.error("{} Malformed Error Json", TAG);
            log.debug("", e);
            message = "Hups. Something went wrong at server-side";
            errorUtils.reportToBugsnag(YalseeErrorBuilder
                    .withTechMessage(String.format("onFailStoreLink: Malformed JSON. Body: %s",
                            response.getBody().toPrettyString()
                    ))
                    .withStatus(response.getStatus()).addRawException(e)
                    .build());
        }

        showError(message);
    }

    private void updateCounter() {
        linkCounter.setText(Long.toString(overallService.numberOfStoredLinks()));
    }

    private int calculateQRCodeSize() {
        int[] browserWidthInfo = new int[1];
        if (getUI().isPresent()) {
            int[] finalBrowserWidthInfo = browserWidthInfo;
            getUI()
                    .get()
                    .getPage()
                    .retrieveExtendedClientDetails(
                            details -> finalBrowserWidthInfo[0] = details.getScreenWidth());
        } else {
            browserWidthInfo = new int[]{0};
        }
        int browserWidth = browserWidthInfo[0];

        int defaultQRBlockSize = App.QR.DEFAULT_QR_BLOCK_SIZE;
        int defaultQRCodeSize = App.QR.DEFAULT_QR_CODE_SIZE;
        float qrBlockRatio = App.QR.QR_BLOCK_RATIO;

        int size;
        if (browserWidth > defaultQRBlockSize) {
            size = defaultQRCodeSize;
        } else {
            size = Math.round(browserWidth * qrBlockRatio);
        }
        return size;
    }

    private void generateQRCode(final String ident) {
        int size = calculateQRCodeSize();
        String qrCodeGeneratorRoute;
        if (size == 0) {
            qrCodeGeneratorRoute =
                    String.format("%s/%s/%s", appUtils.getAPIHostPort(), Endpoint.Api.QR_CODE_API, ident);
        } else {
            qrCodeGeneratorRoute =
                    String.format(
                            "%s/%s/%s/%d", appUtils.getAPIHostPort(), Endpoint.Api.QR_CODE_API, ident, size);
        }

        HttpResponse<JsonNode> response = Unirest.get(qrCodeGeneratorRoute).asJson();
        log.debug("{} Got reply from QR Code API. Status: {}, Body: {}",
                TAG, response.getStatus(), response.getBody().toPrettyString());
        if (response.isSuccess()) {
            onSuccessGenerateQRCode(response);
        } else {
            onFailGenerateQRCode(response);
        }
    }

    private void onSuccessGenerateQRCode(final HttpResponse<JsonNode> response) {
        if (response.getStatus() == STATUS_200) {
            String qrCode = response.getBody().getObject().getString("qr_code");
            if (StringUtils.isNotBlank(qrCode)) {
                this.qrCode.setSrc(qrCode);
                qrCodeArea.setVisible(true);
            } else {
                showError("Internal error. Got malformed reply from QR generator");
                errorUtils.reportToBugsnag(YalseeErrorBuilder
                        .withTechMessage(String.format("onSuccessGenerateQRCode: Malformed JSON. Body: %s",
                                response.getBody().toPrettyString()
                        ))
                        .withStatus(response.getStatus())
                        .build());
            }
        } else {
            showError("Internal error. Something is wrong at server-side");
            errorUtils.reportToBugsnag(YalseeErrorBuilder
                    .withTechMessage(String.format("onSuccessGenerateQRCode: False positive. Body: %s",
                            response.getBody().toPrettyString()
                    ))
                    .withStatus(response.getStatus())
                    .build());
        }
    }

    private void onFailGenerateQRCode(final HttpResponse<JsonNode> response) {
        showError("Internal error. Got malformed reply from QR generator");
        errorUtils.reportToBugsnag(YalseeErrorBuilder
                .withTechMessage(String.format("onFailGenerateQRCode: Malformed JSON. Body: %s",
                        response.getBody().toPrettyString()
                ))
                .withStatus(response.getStatus())
                .build());
        this.qrCode.setSrc("");
        qrCodeArea.setVisible(false);

        if (response.getBody() != null) {
            log.error("{} QR Code Reply JSON: {}", TAG, response.getBody());
        }
    }

    private void showError(final String errorMessage) {
        errorNotification = ErrorUtils.getErrorNotification(errorMessage);
        errorNotification.open();
    }

    private void cleanForm() {
        input.setValue("");
    }

    private void cleanErrors() {
        if (errorNotification != null && errorNotification.isOpened()) {
            errorNotification.close();
        }
    }

    private void cleanResults() {
        shortLink.setHref("");
        shortLink.setText("");
        resultArea.setVisible(false);

        qrCode.setSrc("");
        qrCodeArea.setVisible(false);
    }

    public static class IDs {
        public static final String VIEW_ID = "homeView";
        public static final String MAIN_AREA = "mainArea";
        public static final String TITLE = "siteTitle";
        public static final String INPUT = "longUrlInput";
        public static final String BANNER = "publicAccessBanner";
        public static final String SUBMIT_BUTTON = "submitButton";

        public static final String OVERALL_AREA = "overallArea";
        public static final String OVERALL_LINKS_TEXT = "overallLinksText";
        public static final String OVERALL_LINKS_NUMBER = "overallLinksNum";

        public static final String RESULT_AREA = "resultArea";

        public static final String SHORT_LINK = "shortLink";
        public static final String COPY_LINK_BUTTON = "copyLink";

        public static final String QR_CODE_AREA = "qrCodeArea";
        public static final String QR_CODE = "qrCode";
    }

}
