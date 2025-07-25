package uy.com.equipos.panelmanagement.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import uy.com.equipos.panelmanagement.data.User;
import uy.com.equipos.panelmanagement.security.AuthenticatedUser;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

	private H1 viewTitle;

	private AuthenticatedUser authenticatedUser;
	private AccessAnnotationChecker accessChecker;

	public MainLayout(AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker) {
		this.authenticatedUser = authenticatedUser;
		this.accessChecker = accessChecker;

		setPrimarySection(Section.DRAWER);
		addDrawerContent();
		addHeaderContent();
	}

	private void addHeaderContent() {
		DrawerToggle toggle = new DrawerToggle();
		toggle.setAriaLabel("Menu toggle");

		viewTitle = new H1();
		viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

		addToNavbar(true, toggle, viewTitle);
	}

	private void addDrawerContent() {
		Span appName = new Span("Gestión de Paneles");
		appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
		Header header = new Header(appName);

		Scroller scroller = new Scroller(createNavigation());

		addToDrawer(header, scroller, createFooter());
	}

	private SideNav createNavigation() {
    SideNav nav = new SideNav();

//    // Add "Panelistas Pendientes"
//    SideNavItem pendingPanelistsItem = new SideNavItem("Panelistas Pendientes", "pending-panelists");
//    pendingPanelistsItem.setPrefixComponent(new Icon("vaadin", "bell"));
//    nav.addItem(pendingPanelistsItem);

    // Add other menu items, excluding "Usuarios" and "Solicitudes de panelistas"
    List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
    menuEntries.stream()
        .filter(entry -> !entry.title().equals("Usuarios") && !entry.title().equals("Solicitudes de panelistas"))
        .forEach(entry -> {
        if (entry.icon() != null) {
            nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
        } else {
            nav.addItem(new SideNavItem(entry.title(), entry.path()));
        }
    });

    // Add "Respuestas"
    SideNavItem answersItem = new SideNavItem("Respuestas", "answers");
    answersItem.setPrefixComponent(new Icon("vaadin", "comments-o"));
    nav.addItem(answersItem);

    // Add "Configuración" with "Usuarios" and "Tareas" as sub-items
    SideNavItem configuracionItem = new SideNavItem("Configuración");
    configuracionItem.setPrefixComponent(new Icon("vaadin", "tools"));

    SideNavItem usuariosItem = new SideNavItem("Usuarios", "users");
    usuariosItem.setPrefixComponent(new Icon("vaadin", "users"));
    configuracionItem.addItem(usuariosItem);

    SideNavItem tareasItem = new SideNavItem("Tareas", "tasks");
    tareasItem.setPrefixComponent(new Icon("vaadin", "envelope-o")); // Icon for "Tareas"
    configuracionItem.addItem(tareasItem); // Add "Tareas" under "Configuración"



    SideNavItem recruitmentMessageItem = new SideNavItem("Reclutamiento", "recruitment");
    recruitmentMessageItem.setPrefixComponent(new Icon("vaadin", "megaphone"));
    configuracionItem.addItem(recruitmentMessageItem);
    
    
    SideNavItem systemPropertiesItem = new SideNavItem("Propiedades de sistema", "configuration-items");
    systemPropertiesItem.setPrefixComponent(new Icon("vaadin", "cog-o"));
    configuracionItem.addItem(systemPropertiesItem);

    nav.addItem(configuracionItem);

    return nav;
}

	private Footer createFooter() {
		Footer layout = new Footer();

		Optional<User> maybeUser = authenticatedUser.get();
		if (maybeUser.isPresent()) {
			User user = maybeUser.get();

			Avatar avatar = new Avatar(user.getName());
			byte[] profilePictureData = user.getProfilePicture();
			if (profilePictureData != null) {
				StreamResource resource = new StreamResource("profile-pic",
						() -> new ByteArrayInputStream(profilePictureData));
				avatar.setImageResource(resource);
			}
			avatar.setThemeName("xsmall");
			avatar.getElement().setAttribute("tabindex", "-1");

			MenuBar userMenu = new MenuBar();
			userMenu.setThemeName("tertiary-inline contrast");

			MenuItem userName = userMenu.addItem("");
			Div div = new Div();
			div.add(avatar);
			div.add(user.getName());
			div.add(new Icon("lumo", "dropdown"));
			div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
			userName.add(div);
			userName.getSubMenu().addItem("Sign out", e -> {
				authenticatedUser.logout();
			});

			layout.add(userMenu);
		} else {
			Anchor loginLink = new Anchor("login", "Sign in");
			layout.add(loginLink);
		}

		return layout;
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		viewTitle.setText(getCurrentPageTitle());
	}

	private String getCurrentPageTitle() {
		return MenuConfiguration.getPageHeader(getContent()).orElse("");
	}
}
