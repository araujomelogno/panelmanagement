package uy.com.equipos.panelmanagement.views.users;

import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.Key; // Added for keyboard shortcut
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.PasswordField; // Added for password
import com.vaadin.flow.component.combobox.MultiSelectComboBox; // Added for roles
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import uy.com.equipos.panelmanagement.data.Role; // Added for roles
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.RolesAllowed;
import uy.com.equipos.panelmanagement.data.User;
import uy.com.equipos.panelmanagement.services.UserService;
import org.springframework.data.jpa.domain.Specification;


@PageTitle("Usuarios")
@Route("users/:userID?/:action?(edit)")
@Menu(order = 6, icon = LineAwesomeIconUrl.COLUMNS_SOLID)
@RolesAllowed("ADMIN")
public class UsersView extends Div implements BeforeEnterObserver {

	private final String USER_ID = "userID";
	private final String USER_EDIT_ROUTE_TEMPLATE = "users/%s/edit";

	private final Grid<User> grid = new Grid<>(User.class, false);
	private Div editorLayoutDiv; // Declarado como miembro de la clase

	// Campos de filtro
	private TextField nameFilter = new TextField();
	private TextField usernameFilter = new TextField(); // Changed from emailFilter

	private TextField name;
	private PasswordField passwordField; // Added for password
	private TextField username; // Changed from email
	private MultiSelectComboBox<Role> roles; // Added for roles

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button nuevoUsuarioButton;

	private final BeanValidationBinder<User> binder;

	private User user; // Changed from appUser

	private final UserService userService; // Changed from appUserService

	public UsersView(UserService userService) { // Changed from appUserService
		this.userService = userService; // Changed from appUserService
		addClassNames("users-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		// Configurar columnas del Grid PRIMERO
		grid.addColumn(User::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
		grid.addColumn(User::getUsername).setHeader("Username").setKey("username").setAutoWidth(true); // Changed from getEmail
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
			populateForm(new User()); // Changed from new AppUser()
			if (editorLayoutDiv != null) {
				editorLayoutDiv.setVisible(true);
			}
			if (name != null) {
				name.focus();
			}
		});

		// Configurar placeholders para filtros
		nameFilter.setPlaceholder("Filtrar por Nombre");
		usernameFilter.setPlaceholder("Filtrar por Username"); // Changed from emailFilter

		// Añadir listeners para refrescar el grid
		nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		usernameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll()); // Changed from emailFilter

		// Configurar el DataProvider del Grid
        grid.setItems(query -> {
            String nameVal = nameFilter.getValue();
            String usernameVal = usernameFilter.getValue(); // Changed from emailVal

            // Build Specification for filtering
            Specification<User> spec = Specification.where(null);
            if (nameVal != null && !nameVal.isEmpty()) {
                spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), "%" + nameVal.toLowerCase() + "%"));
            }
            if (usernameVal != null && !usernameVal.isEmpty()) {
                spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("username")), "%" + usernameVal.toLowerCase() + "%"));
            }

            return userService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), spec).stream(); // Changed from appUserService.list
        });


		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(USER_EDIT_ROUTE_TEMPLATE, event.getValue().getId())); // Changed from APPUSER_EDIT_ROUTE_TEMPLATE
			} else {
				clearForm(); // clearForm ahora también oculta el editor
				UI.getCurrent().navigate(UsersView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(User.class); // Changed from AppUser.class

		// Bind fields. This is where you'd define e.g. validation rules
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.user == null) { // Changed from this.appUser
					this.user = new User(); // Changed from new AppUser()
				}
				binder.writeBean(this.user); // Changed from this.appUser

				String plainPassword = passwordField.getValue();
				userService.save(this.user, plainPassword); // Pass plain password to service

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
		save.addClickShortcut(Key.ENTER);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> userId = event.getRouteParameters().get(USER_ID).map(Long::parseLong); // Changed from appUserId / APPUSER_ID
		if (userId.isPresent()) {
			Optional<User> userFromBackend = userService.get(userId.get()); // Changed from appUserFromBackend / appUserService.get
			if (userFromBackend.isPresent()) {
				populateForm(userFromBackend.get());
				editorLayoutDiv.setVisible(true);
			} else {
				Notification.show(String.format("El usuario solicitado no fue encontrado, ID = %s", userId.get()), // Changed from appUserId.get()
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
		username = new TextField("Username"); // Changed from email / "Correo Electrónico"
		passwordField = new PasswordField("Contraseña");
		passwordField.setHelperText("Dejar en blanco para no cambiar la contraseña actual al editar.");

		roles = new MultiSelectComboBox<>("Roles");
		roles.setItems(Role.values());
		roles.setItemLabelGenerator(Role::name);

		formLayout.add(name, username, passwordField, roles);

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
		headerRow.getCell(grid.getColumnByKey("username")).setComponent(usernameFilter); // Changed from email / emailFilter
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void populateForm(User value) { // Changed from AppUser
		this.user = value; // Changed from this.appUser
		binder.readBean(this.user); // Changed from this.appUser

		// Clear password field when populating the form for an existing user
		if (passwordField != null) {
			passwordField.clear();
		}

		if (deleteButton != null) { 
			 deleteButton.setEnabled(value != null && value.getId() != null);
		}
	}

	private void clearForm() {
		populateForm(null); // This will also clear the password field
		if (editorLayoutDiv != null) { // Buena práctica verificar nulidad
			editorLayoutDiv.setVisible(false);
		}
		if (deleteButton != null) {
			deleteButton.setEnabled(false);
		}
	}

	private void onDeleteClicked() {
		if (this.user == null || this.user.getId() == null) { // Changed from this.appUser
			Notification.show("No hay usuario seleccionado para eliminar.", 3000, Notification.Position.MIDDLE);
			return;
		}

		com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
		dialog.setHeader("Confirmar Eliminación");
		dialog.setText("¿Está seguro de que desea eliminar el usuario '" + this.user.getName() + "'?"); // Changed from this.appUser.getName()
		
		dialog.setConfirmText("Eliminar");
		dialog.setConfirmButtonTheme("error primary");
		dialog.setCancelText("Cancelar");

		dialog.addConfirmListener(event -> {
			try {
				userService.delete(this.user.getId()); // Changed from appUserService.delete(this.appUser.getId())
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
