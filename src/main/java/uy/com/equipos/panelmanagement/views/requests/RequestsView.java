package uy.com.equipos.panelmanagement.views.requests;

import java.util.Optional;

import org.springframework.data.domain.Pageable; // Added import
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow; // Added import
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.PermitAll;
import uy.com.equipos.panelmanagement.data.Request;
import uy.com.equipos.panelmanagement.services.RequestService;

@PageTitle("Solicitudes de panelistas")
@Route("requests/:requestID?/:action?(edit)")
@Menu(order = 5, icon = LineAwesomeIconUrl.BELL)
@PermitAll
public class RequestsView extends Div implements BeforeEnterObserver {

	private final String REQUEST_ID = "requestID";
	private final String REQUEST_EDIT_ROUTE_TEMPLATE = "requests/%s/edit";

	private final Grid<Request> grid = new Grid<>(Request.class, false);
	private Div editorLayoutDiv; // Declarado como miembro de la clase

	// Form fields
	private TextField firstName;
	private TextField lastName;
	private TextField birhtdate; // Field in entity is 'birhtdate'
	private TextField sex;
	private TextField email;
	private TextField phone;

	// Filter fields
	private TextField firstNameFilter = new TextField();
	private TextField lastNameFilter = new TextField();
	private TextField birhtdateFilter = new TextField();
	private TextField sexFilter = new TextField();
	private TextField emailFilter = new TextField();
	private TextField phoneFilter = new TextField();

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button nuevaSolicitudButton;

	private final BeanValidationBinder<Request> binder;

	private Request request;

	private final RequestService requestService;

	public RequestsView(RequestService requestService) {
		this.requestService = requestService;
		addClassNames("requests-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		// Create UI
		SplitLayout splitLayout = new SplitLayout();

		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);
		// editorLayoutDiv.setVisible(false); // Se maneja después de add(mainLayout)

		nuevaSolicitudButton = new Button("Nueva Solicitud");
		nuevaSolicitudButton.getStyle().set("margin-left", "18px"); 
		VerticalLayout mainLayout = new VerticalLayout(nuevaSolicitudButton, splitLayout);
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);

		add(mainLayout);
		if (this.editorLayoutDiv != null) {
			this.editorLayoutDiv.setVisible(false);
		}

		// Listener para el botón "Nueva Solicitud"
		nuevaSolicitudButton.addClickListener(click -> {
			grid.asSingleSelect().clear();
			populateForm(new Request());
			if (editorLayoutDiv != null) {
				editorLayoutDiv.setVisible(true);
			}
			if (firstName != null) {
				firstName.focus();
			}
		});

		// Configure Grid
		Grid.Column<Request> firstNameCol = grid.addColumn(Request::getFirstName).setHeader("Nombre").setKey("firstName").setAutoWidth(true);
		Grid.Column<Request> lastNameCol = grid.addColumn(Request::getLastName).setHeader("Apellido").setKey("lastName").setAutoWidth(true);
		Grid.Column<Request> birhtdateCol = grid.addColumn(Request::getBirhtdate).setHeader("Fecha de Nacimiento").setKey("birhtdate").setAutoWidth(true);
		Grid.Column<Request> sexCol = grid.addColumn(Request::getSex).setHeader("Sexo").setKey("sex").setAutoWidth(true);
		Grid.Column<Request> emailCol = grid.addColumn(Request::getEmail).setHeader("Correo Electrónico").setKey("email").setAutoWidth(true);
		Grid.Column<Request> phoneCol = grid.addColumn(Request::getPhone).setHeader("Teléfono").setKey("phone").setAutoWidth(true);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// Add filter row
		HeaderRow filterRow = grid.appendHeaderRow();

		firstNameFilter.setPlaceholder("Filtrar por Nombre...");
		filterRow.getCell(firstNameCol).setComponent(firstNameFilter);

		lastNameFilter.setPlaceholder("Filtrar por Apellido...");
		filterRow.getCell(lastNameCol).setComponent(lastNameFilter);

		birhtdateFilter.setPlaceholder("Filtrar por Fecha Nac...");
		filterRow.getCell(birhtdateCol).setComponent(birhtdateFilter);

		sexFilter.setPlaceholder("Filtrar por Sexo...");
		filterRow.getCell(sexCol).setComponent(sexFilter);

		emailFilter.setPlaceholder("Filtrar por Email...");
		filterRow.getCell(emailCol).setComponent(emailFilter);

		phoneFilter.setPlaceholder("Filtrar por Teléfono...");
		filterRow.getCell(phoneCol).setComponent(phoneFilter);

		// Add listeners to filter fields
		firstNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		lastNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		birhtdateFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		sexFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		emailFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		phoneFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

		grid.setItems(query -> {
            Pageable pageable = VaadinSpringDataHelpers.toSpringPageRequest(query);
            String fName = firstNameFilter.getValue();
            String lName = lastNameFilter.getValue();
            String bDate = birhtdateFilter.getValue();
            String sSex = sexFilter.getValue();
            String mail = emailFilter.getValue();
            String ph = phoneFilter.getValue();
            return requestService.list(pageable, fName, lName, bDate, sSex, mail, ph).stream();
        });

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				// this.editorLayoutDiv.setVisible(true); // Removido: beforeEnter lo manejará
				UI.getCurrent().navigate(String.format(REQUEST_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm(); // clearForm ahora también oculta el editor
				UI.getCurrent().navigate(RequestsView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(Request.class);

		// Bind fields. This is where you'd define e.g. validation rules

		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.request == null) {
					this.request = new Request();
				}
				binder.writeBean(this.request);
				requestService.save(this.request);
				clearForm();
				refreshGrid();
				Notification.show("Datos actualizados");
				UI.getCurrent().navigate(RequestsView.class);
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
		Optional<Long> requestId = event.getRouteParameters().get(REQUEST_ID).map(Long::parseLong);
		if (requestId.isPresent()) {
			Optional<Request> requestFromBackend = requestService.get(requestId.get());
			if (requestFromBackend.isPresent()) {
				populateForm(requestFromBackend.get());
				if (this.editorLayoutDiv != null) {
					this.editorLayoutDiv.setVisible(true);
				}
			} else {
				Notification.show(String.format("La solicitud no fue encontrada, ID = %s", requestId.get()), 3000,
						Notification.Position.BOTTOM_START);
				// when a row is selected but the data is no longer available,
				// refresh grid
				refreshGrid();
				if (this.editorLayoutDiv != null) {
					this.editorLayoutDiv.setVisible(false);
				}
				event.forwardTo(RequestsView.class);
			}
		} else {
			clearForm(); // Asegurar que el editor esté oculto si no hay ID
		}
	}

	private void createEditorLayout(SplitLayout splitLayout) {
		this.editorLayoutDiv = new Div(); // Instanciar el miembro de la clase
		this.editorLayoutDiv.setClassName("editor-layout");

		Div editorDiv = new Div();
		editorDiv.setClassName("editor");
		this.editorLayoutDiv.add(editorDiv);

		FormLayout formLayout = new FormLayout();
		firstName = new TextField("Nombre");
		lastName = new TextField("Apellido");
		birhtdate = new TextField("Fecha de Nacimiento");
		sex = new TextField("Sexo");
		email = new TextField("Correo Electrónico");
		phone = new TextField("Teléfono");
		formLayout.add(firstName, lastName, birhtdate, sex, email, phone);

		editorDiv.add(formLayout);
		createButtonLayout(this.editorLayoutDiv);

		splitLayout.addToSecondary(this.editorLayoutDiv);
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
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void populateForm(Request value) {
		this.request = value;
		binder.readBean(this.request);

		if (deleteButton != null) { 
			 deleteButton.setEnabled(value != null && value.getId() != null);
		}
	}

	private void clearForm() {
		populateForm(null);
		if (this.editorLayoutDiv != null) { // Buena práctica verificar nulidad
			this.editorLayoutDiv.setVisible(false);
		}
		if (deleteButton != null) {
			deleteButton.setEnabled(false);
		}
	}

	private void onDeleteClicked() {
		if (this.request == null || this.request.getId() == null) {
			Notification.show("No hay solicitud seleccionada para eliminar.", 3000, Notification.Position.MIDDLE);
			return;
		}

		com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
		dialog.setHeader("Confirmar Eliminación");
		dialog.setText("¿Está seguro de que desea eliminar la solicitud de '" + this.request.getFirstName() + " " + this.request.getLastName() + "'?");
		
		dialog.setConfirmText("Eliminar");
		dialog.setConfirmButtonTheme("error primary");
		dialog.setCancelText("Cancelar");

		dialog.addConfirmListener(event -> {
			try {
				requestService.delete(this.request.getId());
				clearForm();
				refreshGrid();
				Notification.show("Solicitud eliminada correctamente.", 3000, Notification.Position.BOTTOM_START);
				UI.getCurrent().navigate(RequestsView.class);
			} catch (org.springframework.dao.DataIntegrityViolationException ex) {
				Notification.show("No se puede eliminar la solicitud. Es posible que esté siendo referenciada por otras entidades.", 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (Exception ex) {
				Notification.show("Ocurrió un error al intentar eliminar la solicitud: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
		dialog.open();
	}
	// Confirmando estado final de RequestsView con lógica de visibilidad y
	// traducciones.
}
