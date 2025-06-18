package uy.com.equipos.panelmanagement.views.requests;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
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

	private TextField firstName;
	private TextField lastName;
	private TextField birhtdate;
	private TextField sex;
	private TextField email;
	private TextField phone;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button nuevaSolicitudButton;

	private final BeanValidationBinder<Request> binder;

	private Request request;

	private final RequestService requestService;

	public RequestsView(RequestService requestService) {
		this.requestService = requestService;
		addClassNames("requests-view");

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
		grid.addColumn("firstName").setAutoWidth(true);
		grid.addColumn("lastName").setAutoWidth(true);
		grid.addColumn("birhtdate").setAutoWidth(true);
		grid.addColumn("sex").setAutoWidth(true);
		grid.addColumn("email").setAutoWidth(true);
		grid.addColumn("phone").setAutoWidth(true);
		grid.setItems(query -> requestService.list(VaadinSpringDataHelpers.toSpringPageRequest(query)).stream());
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

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
		buttonLayout.add(save, cancel);
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

	private void clearForm() {
		populateForm(null);
		if (this.editorLayoutDiv != null) { // Buena práctica verificar nulidad
			this.editorLayoutDiv.setVisible(false);
		}
	}

	private void populateForm(Request value) {
		this.request = value;
		binder.readBean(this.request);

	}
	// Confirmando estado final de RequestsView con lógica de visibilidad y
	// traducciones.
}
