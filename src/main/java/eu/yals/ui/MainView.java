package eu.yals.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import eu.yals.ui.dev.AppInfoView;
import eu.yals.utils.AppUtils;

import java.util.HashMap;
import java.util.Map;

@SpringComponent
@UIScope
@Push
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@PWA(
        name = "Yet another link shortener",
        shortName = "yals",
        offlinePath = "offline-page.html",
        offlineResources = {"images/logo.png"},
        description = "Yet another link shortener for friends")
@Theme(value = Lumo.class, variant = Lumo.LIGHT)
public class MainView extends AppLayout implements BeforeEnterObserver, PageConfigurator {

    private final Tabs tabs = new Tabs();
    private final Map<Class<? extends Component>, Tab> targets = new HashMap<>();

    /**
     * Creates Main UI.
     *
     * @param appUtils application utils for determine dev mode
     */
    public MainView(final AppUtils appUtils) {

        DrawerToggle toggle = new DrawerToggle();

        Span title = new Span("Site Title".toUpperCase());
        title.setHeight("63px"); //TODO magic number - set by css
        addToNavbar(toggle, title);

        //items
        addMenuTab("Main", HomeView.class);
        addMenuTab("App", AppInfoView.class);

        // dev-only items
        if (appUtils.isDevelopmentModeActivated()) {
            addMenuTab("Debug", DebugView.class);
        }

        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        addToDrawer(tabs);
    }

    private void addMenuTab(String label, Class<? extends Component> target) {
        RouterLink link = new RouterLink(null, target);
        link.add(VaadinIcon.FLASK.create());
        link.add(label);
        Tab tab = new Tab(link);
        targets.put(target, tab);
        tabs.add(tab);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        tabs.setSelectedTab(targets.get(beforeEnterEvent.getNavigationTarget()));
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addFavIcon("icon", "/icons/favicon-32x32.png", "32x32");
        settings.addLink("shortcut icon", "/icons/favicon-16x16.png");
        settings.addLink("apple-touch-icon", "/icons/apple-touch-icon.png");
        settings.addLink("manifest", "/site.webmanifest");
        settings.addLink("mask-icon", "/icons/safari-pinned-tab.svg");

        settings.addMetaTag("apple-mobile-web-app-title", "Yals");
        settings.addMetaTag("application-name", "Yals");
        settings.addMetaTag("msapplication-TileColor", "#ffc40d");
        settings.addMetaTag("theme-color", "#ffffff");
    }
}
