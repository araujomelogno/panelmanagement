package uy.com.equipos.panelmanagement.views.users;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog; // Added import
import org.springframework.dao.DataIntegrityViolationException; // Added import
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.RolesAllowed;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import uy.com.equipos.panelmanagement.data.AppUser;
import uy.com.equipos.panelmanagement.services.AppUserService;

@PageTitle("Usuarios")
@Route("users/:appUserID?/:action?(edit)")
@Menu(order = 6, icon = LineAwesomeIconUrl.COLUMNS_SOLID)
@RolesAllowed("ADMIN")
public class UsersView extends Div implements BeforeEnterObserver {

	private final String APPUSER_ID = "appUserID";
	private final String APPUSER_EDIT_ROUTE_TEMPLATE = "users/%s/edit";

	private final Grid<AppUser> grid = new Grid<>(AppUser.class, false);
	private Div editorLayoutDiv; // Declarado como miembro de la clase

	// Campos de filtro
	private TextField nameFilter = new TextField();
	private TextField emailFilter = new TextField();

	private TextField name;
	private TextField password;
	private TextField email;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button nuevoUsuarioButton;

	private final BeanValidationBinder<AppUser> binder;

	private AppUser appUser;

	private final AppUserService appUserService;

	public UsersView(AppUserService appUserService) {
		this.appUserService = appUserService;
		addClassNames("users-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		// Configurar columnas del Grid PRIMERO
		grid.addColumn(AppUser::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
		grid.addColumn(AppUser::getPassword).setHeader("Contraseña").setAutoWidth(true);
		grid.addColumn(AppUser::getEmail).setHeader("Correo Electrónico").setKey("email").setAutoWidth(true);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// Create UI - SplitLayout
		SplitLayout splitLayout = new SplitLayout();
		// createGridLayout ahora puede acceder a las keys de las columnas de forma
		// segura
		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);
		// editorLayoutDiv.setVisible(false); // Se maneja después de add(mainLayout)

		nuevoUsuarioButton = new Button("Nuevo Usuario");
		nuevoUsuarioButton.getStyle().set("margin-left", "18px");   

		VerticalLayout mainLayout = new VerticalLayout(nuevoUsuarioButton, splitLayout);
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);

		add(mainLayout);
		if (editorLayoutDiv != null) {
			editorLayoutDiv.setVisible(false);
		}

		// Listener para el botón "Nuevo Usuario"
		nuevoUsuarioButton.addClickListener(click -> {
			grid.asSingleSelect().clear();
			populateForm(new AppUser());
			if (editorLayoutDiv != null) {
				editorLayoutDiv.setVisible(true);
			}
			if (name != null) {
				name.focus();
			}
		});

		// Configurar placeholders para filtros
		nameFilter.setPlaceholder("Filtrar por Nombre");
		emailFilter.setPlaceholder("Filtrar por Correo Electrónico");

		// Añadir listeners para refrescar el grid
		nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		emailFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

		// Configurar el DataProvider del Grid
		grid.setItems(query -> {
			String nameVal = nameFilter.getValue();
			String emailVal = emailFilter.getValue();
			return appUserService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), nameVal, emailVal).stream();
		});

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(APPUSER_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm(); // clearForm ahora también oculta el editor
				UI.getCurrent().navigate(UsersView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(AppUser.class);

		// Bind fields. This is where you'd define e.g. validation rules
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.appUser == null) {
					this.appUser = new AppUser();
				}
				binder.writeBean(this.appUser);
				appUserService.save(this.appUser);
				clearForm();
				refreshGrid();
				Notification.show("Datos actualizados");
				UI.getCurrent().navigate(UsersView.class);
			} catch (ObjectOptimisticLockingFailureException exception) {
				Notification n = Notification.show(
						"Error al actualizar los datos. Otro usuario modificó el registro mientras usted realizaba cambios.");
				n.setPosition(Position.MIDDLE);
				n.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (ValidationException validationException) {
				Notification
						.show("Fallo al actualizar los datos. Verifique nuevamente que todos los valores sean válidos");
			}
		});
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> appUserId = event.getRouteParameters().get(APPUSER_ID).map(Long::parseLong);
		if (appUserId.isPresent()) {
			Optional<AppUser> appUserFromBackend = appUserService.get(appUserId.get());
			if (appUserFromBackend.isPresent()) {
				populateForm(appUserFromBackend.get());
				editorLayoutDiv.setVisible(true);
			} else {
				Notification.show(String.format("El usuario solicitado no fue encontrado, ID = %s", appUserId.get()),
						3000, Notification.Position.BOTTOM_START);
				// when a row is selected but the data is no longer available,
				// refresh grid
				refreshGrid();
				if (editorLayoutDiv != null) {
					editorLayoutDiv.setVisible(false);
				}
				event.forwardTo(UsersView.class);
			}
		} else {
			clearForm(); // Asegurar que el editor esté oculto si no hay ID
		}
	}

	private void createEditorLayout(SplitLayout splitLayout) {
		editorLayoutDiv = new Div(); // Instanciar el miembro de la clase
		editorLayoutDiv.setClassName("editor-layout");

		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		editorLayoutDiv.add(editorDiv);

		FormLayout formLayout = new FormLayout();
		name = new TextField("Nombre");
		password = new TextField("Contraseña");
		email = new TextField("Correo Electrónico");
		formLayout.add(name, password, email);

		editorDiv.add(formLayout);
		createButtonLayout(editorLayoutDiv);

		splitLayout.addToSecondary(editorLayoutDiv);
	}

	private void createButtonLayout(Div editorLayoutDiv) {
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setClassName("button-layout");
		cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		buttonLayout.add(save, deleteButton, cancel);
		editorLayoutDiv.add(buttonLayout);
	}

	private void createGridLayout(SplitLayout splitLayout) {
		Div wrapper = new Div();
		wrapper.setClassName("grid-wrapper");
		splitLayout.addToPrimary(wrapper);
		wrapper.add(grid);

		HeaderRow headerRow = grid.appendHeaderRow();
		headerRow.getCell(grid.getColumnByKey("name")).setComponent(nameFilter);
		headerRow.getCell(grid.getColumnByKey("email")).setComponent(emailFilter);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void populateForm(AppUser value) {
		this.appUser = value;
		binder.readBean(this.appUser);

		if (deleteButton != null) {
			 deleteButton.setEnabled(value != null && value.getId() != null);
		}
	}

	private void clearForm() {
		populateForm(null);
		if (editorLayoutDiv != null) { // Buena práctica verificar nulidad
			editorLayoutDiv.setVisible(false);
		}
		if (deleteButton != null) {
			deleteButton.setEnabled(false);
		}
	}

	private void onDeleteClicked() {
		if (this.appUser == null || this.appUser.getId() == null) {
			Notification.show("No hay usuario seleccionado para eliminar.", 3000, Notification.Position.MIDDLE);
			return;
		}

		com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
		dialog.setHeader("Confirmar Eliminación");
		dialog.setText("¿Está seguro de que desea eliminar el usuario '" + this.appUser.getName() + "'?");

		dialog.setConfirmText("Eliminar");
		dialog.setConfirmButtonTheme("error primary");
		dialog.setCancelText("Cancelar");

		dialog.addConfirmListener(event -> {
			try {
				appUserService.delete(this.appUser.getId());
				clearForm();
				refreshGrid();
				Notification.show("Usuario eliminado correctamente.", 3000, Notification.Position.BOTTOM_START);
				UI.getCurrent().navigate(UsersView.class);
			} catch (org.springframework.dao.DataIntegrityViolationException ex) {
				Notification.show("No se puede eliminar el usuario. Es posible que esté siendo referenciado por otras entidades o tenga roles asignados.", 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (Exception ex) {
				Notification.show("Ocurrió un error al intentar eliminar el usuario: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
		dialog.open();
	}
}
