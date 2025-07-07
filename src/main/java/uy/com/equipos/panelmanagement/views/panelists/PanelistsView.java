package uy.com.equipos.panelmanagement.views.panelists;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap; // Added
import java.util.HashSet;
import java.util.List; // Make sure this is present
import java.util.Locale;
import java.util.Map; // Added
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors; // Added/Uncommented

import org.apache.commons.lang3.StringUtils; // Added for filtering
import org.springframework.data.jpa.domain.Specification; // Added import for Specification
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key; // Added for keyboard shortcut
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
// import com.vaadin.flow.component.grid.HeaderRow; // Will be used for filter row
import com.vaadin.flow.component.checkbox.Checkbox; // Added for checkbox column
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog; // Added import
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid; // Already present, but good to confirm
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow; // Added for filtering
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField; // Likely already present
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider; // Added for filtering
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCode;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCodeRepository;
import uy.com.equipos.panelmanagement.data.PanelistPropertyValue; // Added
import uy.com.equipos.panelmanagement.data.PropertyType;
import uy.com.equipos.panelmanagement.services.PanelService; // Added
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;
import uy.com.equipos.panelmanagement.services.PanelistPropertyValueService;
import uy.com.equipos.panelmanagement.services.PanelistService;
import uy.com.equipos.panelmanagement.views.dialogs.PanelistResultsDialog; // Importar el nuevo diálogo
import uy.com.equipos.panelmanagement.views.panels.PanelistPropertyFilterDialog; // Importar la clase del diálogo

@PageTitle("Panelistas")
@Route("panelists/:panelistID?/:action?(edit)")
@Menu(order = 1, icon = LineAwesomeIconUrl.USER_SOLID)
@PermitAll
public class PanelistsView extends Div implements BeforeEnterObserver {

	private final String PANELIST_ID = "panelistID";
	private final String PANELIST_EDIT_ROUTE_TEMPLATE = "panelists/%s/edit";

	private final Grid<Panelist> grid = new Grid<>(Panelist.class, false);
	private Div editorLayoutDiv; // Declarado como miembro de la clase

	// Campos de filtro
	private TextField firstNameFilter = new TextField();
	private TextField lastNameFilter = new TextField();
	private TextField emailFilter = new TextField();
	private TextField phoneFilter = new TextField();
	// private DatePicker dateOfBirthFilter = new DatePicker(); // Removed
	// private TextField occupationFilter = new TextField(); // Removed
	private DatePicker lastContactedFilter = new DatePicker();
	private DatePicker lastInterviewCompletedFilter = new DatePicker();
	private TextField sourceFilter = new TextField(); // Added for source filter

	private TextField firstName;
	private TextField lastName;
	private TextField email;
	private TextField phone;
	private TextField source; // Added for editor form
	// private DatePicker dateOfBirth; // Removed
	// private TextField occupation; // Removed
	private DatePicker lastContacted;
	private DatePicker lastInterviewCompleted;
	// private MultiSelectListBox<PanelistProperty> propertiesField; // Removed
	private Button gestionarPropiedadesButton; // Added

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button nuevoPanelistaButton;
	// private Button gestionarPropiedadesButton; // Removed duplicate declaration
	private Button viewParticipatingPanelsButton;
	private Button viewParticipatingSurveysButton; // Added
	Dialog viewPanelsDialog; // Package-private for testing
	Grid<Panel> participatingPanelsGrid; // Package-private for testing
	Panelist currentPanelistForPanelsDialog; // Package-private for testing
	Set<Panel> modifiedPanelsInDialog; // Package-private for testing, holds state for the dialog
	// Fields for testing dialog buttons and filters
	Button savePanelsButtonDialog; // Package-private for testing
	Button cancelPanelsButtonDialog; // Package-private for testing
	TextField namePanelFilterDialog; // Package-private for testing
	TextField createdPanelFilterDialog; // Package-private for testing
	ComboBox<String> activePanelFilterDialog; // Package-private for testing

	private final BeanValidationBinder<Panelist> binder;

	private Panelist panelist;

	private final PanelistService panelistService;
	private final PanelistPropertyService panelistPropertyService; // Re-added
	private final PanelistPropertyValueService panelistPropertyValueService; // Added new
	private final PanelistPropertyCodeRepository panelistPropertyCodeRepository;
	private final PanelService panelService; // Added
	private Dialog gestionarPropiedadesDialog; // Add new

	public PanelistsView(PanelistService panelistService, PanelistPropertyService panelistPropertyService,
			PanelistPropertyValueService panelistPropertyValueService,
			PanelistPropertyCodeRepository panelistPropertyCodeRepository, PanelService panelService) { // Added
																										// panelService
		this.panelistService = panelistService;
		this.panelistPropertyService = panelistPropertyService; // Re-added
		this.panelistPropertyValueService = panelistPropertyValueService; // Added new
		this.panelistPropertyCodeRepository = panelistPropertyCodeRepository;
		this.panelService = panelService; // Added
		addClassNames("panelists-view");

		viewParticipatingSurveysButton = new Button("Ver encuestas participadas"); // Added
		viewParticipatingSurveysButton.addClickListener(e -> openParticipatingSurveysDialog()); // Added

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		viewParticipatingPanelsButton = new Button("Ver paneles en los que participa");
		viewParticipatingPanelsButton.addClickListener(e -> {
			if (this.panelist != null && this.panelist.getId() != null) {
				this.currentPanelistForPanelsDialog = this.panelist;
				createOrOpenViewPanelsDialog();
			} else {
				Notification.show("Seleccione un panelista para ver sus paneles.", 3000, Notification.Position.MIDDLE);
			}
		});

		// Configurar columnas del Grid PRIMERO
		grid.addColumn(Panelist::getFirstName).setHeader("Nombre").setKey("firstName").setAutoWidth(true).setSortable(true);
		grid.addColumn(Panelist::getLastName).setHeader("Apellido").setKey("lastName").setAutoWidth(true).setSortable(true);
		grid.addColumn(Panelist::getEmail).setHeader("Correo Electrónico").setKey("email").setAutoWidth(true).setSortable(true);
		grid.addColumn(Panelist::getPhone).setHeader("Teléfono").setKey("phone").setAutoWidth(true).setSortable(true);
		// grid.addColumn(Panelist::getDateOfBirth).setHeader("Fecha de
		// Nacimiento").setKey("dateOfBirth")
		// .setAutoWidth(true); // Removed
		// grid.addColumn(Panelist::getOccupation).setHeader("Ocupación").setKey("occupation").setAutoWidth(true);
		// // Removed
		grid.addColumn(Panelist::getLastContacted).setHeader("Último encuesta enviada").setKey("lastContacted")
				.setAutoWidth(true).setSortable(true);
		grid.addColumn(Panelist::getLastInterviewCompleted).setHeader("Última encuesta completa").setKey("lastInterviewCompleted")
				.setAutoWidth(true).setSortable(true);
		grid.addColumn(Panelist::getSource).setHeader("Fuente").setKey("source").setAutoWidth(true).setSortable(true);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// Create UI - SplitLayout
		SplitLayout splitLayout = new SplitLayout();
		// createGridLayout ahora puede acceder a las keys de las columnas de forma
		// segura
		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);
		// editorLayoutDiv.setVisible(false); // Se maneja después de add(mainLayout)

		// Crear barra de título
		nuevoPanelistaButton = new Button("Nuevo Panelista");
		nuevoPanelistaButton.getStyle().set("margin-left", "18px");

		Button filtrarPanelistasButton = new Button("Filtrar Panelistas");
		filtrarPanelistasButton.addClickListener(e -> {
			PanelistPropertyFilterDialog filterDialog = new PanelistPropertyFilterDialog(panelistPropertyService,
					panelistPropertyCodeRepository, panelService, panelistService, null, // currentPanel es null aquí ya
																							// que el filtro es global,
																							// no para un panel
																							// específico
					filterCriteria -> { // Implementación del SearchListener
						// Aquí se reciben los criterios del PanelistPropertyFilterDialog
						// Abrir el segundo diálogo (ResultsDialog) pasando estos criterios
						openPanelistResultsDialog(filterCriteria);
					});
			filterDialog.open();
		});

		HorizontalLayout buttonBar = new HorizontalLayout(nuevoPanelistaButton, filtrarPanelistasButton);
		buttonBar.setAlignItems(Alignment.CENTER); // Alinea los botones verticalmente si tienen diferentes alturas
		buttonBar.getStyle().set("margin-left", "18px");

		VerticalLayout mainLayout = new VerticalLayout(buttonBar, splitLayout);
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);

		add(mainLayout);
		if (editorLayoutDiv != null) {
			editorLayoutDiv.setVisible(false);
		}

		// Listener para el botón "Nuevo Panelista"
		nuevoPanelistaButton.addClickListener(click -> {
			grid.asSingleSelect().clear();
			populateForm(new Panelist());
			if (editorLayoutDiv != null) {
				editorLayoutDiv.setVisible(true);
			}
			if (firstName != null) {
				firstName.focus();
			}
		});

		// Configurar placeholders para filtros
		firstNameFilter.setPlaceholder("Filtrar por Nombre");
		lastNameFilter.setPlaceholder("Filtrar por Apellido");
		emailFilter.setPlaceholder("Filtrar por Correo Electrónico");
		phoneFilter.setPlaceholder("Filtrar por Teléfono");
		// dateOfBirthFilter.setPlaceholder("Filtrar por Fecha de Nacimiento"); //
		// Removed
		// occupationFilter.setPlaceholder("Filtrar por Ocupación"); // Removed
		lastContactedFilter.setPlaceholder("dd/MM/yyyy");
		lastContactedFilter.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n lastContactedI18n = new DatePicker.DatePickerI18n();
		lastContactedI18n.setDateFormat("dd/MM/yyyy");
		lastContactedFilter.setI18n(lastContactedI18n);

		lastInterviewCompletedFilter.setPlaceholder("dd/MM/yyyy");
		lastInterviewCompletedFilter.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n lastInterviewCompletedI18n = new DatePicker.DatePickerI18n();
		lastInterviewCompletedI18n.setDateFormat("dd/MM/yyyy");
		lastInterviewCompletedFilter.setI18n(lastInterviewCompletedI18n);

		sourceFilter.setPlaceholder("Filtrar por Fuente"); // Added placeholder for source filter

		// Añadir listeners para refrescar el grid
		firstNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		lastNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		emailFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		phoneFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		lastContactedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		lastInterviewCompletedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		sourceFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll()); // Added listener for source
																						// filter

		// Configurar el DataProvider del Grid
		grid.setItems(query -> {
			String firstNameVal = firstNameFilter.getValue();
			String lastNameVal = lastNameFilter.getValue();
			String emailVal = emailFilter.getValue();
			String phoneVal = phoneFilter.getValue();
			// LocalDate dateOfBirthVal = dateOfBirthFilter.getValue(); // Removed
			// String occupationVal = occupationFilter.getValue(); // Removed
			LocalDate lastContactedVal = lastContactedFilter.getValue();
			LocalDate lastInterviewCompletedVal = lastInterviewCompletedFilter.getValue();
			String sourceVal = sourceFilter.getValue(); // Added source value

			// Adjust the call to panelistService.list to exclude dateOfBirthVal and
			// occupationVal
			// This assumes the service method will be updated accordingly in a later step.
			// For now, we'll pass null or an equivalent that the current service method
			// might expect,
			// or if the service method is overloaded or flexible.
			// Based on the current plan, the service method signature would change.
			// Let's assume the service method will be updated to:
			// list(Pageable, String, String, String, String, LocalDate, LocalDate, String)
			// For now, I will modify the Specification within this lambda.
			// The panelistService.list method using specific parameters will need to be
			// updated or this will rely on a Specification-based list method.
			// The current panelistService.list takes specific parameters. I will need to
			// update it or use a Specification variant.
			// For now, let's assume a Specification-based approach is preferred for
			// flexibility.
			// If panelistService.list(Pageable, Specification) exists and is used, this is
			// fine.
			// The existing code uses: panelistService.list(Pageable, String, String,
			// String, String, LocalDate, LocalDate)
			// This means I MUST update the service layer. I will do that in the next
			// sub-step.
			// For now, I will prepare the Specification here.

			Specification<Panelist> spec = (root, cbQuery, cb) -> {
				List<Predicate> predicates = new ArrayList<>();
				if (StringUtils.isNotBlank(firstNameVal)) {
					predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + firstNameVal.toLowerCase() + "%"));
				}
				if (StringUtils.isNotBlank(lastNameVal)) {
					predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + lastNameVal.toLowerCase() + "%"));
				}
				if (StringUtils.isNotBlank(emailVal)) {
					predicates.add(cb.like(cb.lower(root.get("email")), "%" + emailVal.toLowerCase() + "%"));
				}
				if (StringUtils.isNotBlank(phoneVal)) {
					predicates.add(cb.like(cb.lower(root.get("phone")), "%" + phoneVal.toLowerCase() + "%"));
				}
				if (lastContactedVal != null) {
					predicates.add(cb.equal(root.get("lastContacted"), lastContactedVal));
				}
				if (lastInterviewCompletedVal != null) {
					predicates.add(cb.equal(root.get("lastInterviewCompleted"), lastInterviewCompletedVal));
				}
				if (StringUtils.isNotBlank(sourceVal)) {
					predicates.add(cb.like(cb.lower(root.get("source")), "%" + sourceVal.toLowerCase() + "%"));
				}
				return cb.and(predicates.toArray(new Predicate[0]));
			};
			return panelistService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), spec).stream();
		});

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(PANELIST_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm(); // clearForm ahora también oculta el editor
				UI.getCurrent().navigate(PanelistsView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(Panelist.class);

		// Bind fields. This is where you'd define e.g. validation rules
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.panelist == null) {
					this.panelist = new Panelist();
				}

				// Primero intentar escribir los datos del binder para asegurar que el email está disponible
				binder.writeBean(this.panelist);

				// Verificar si es un nuevo panelista y si el correo electrónico ya existe
				if (this.panelist.getId() == null && this.panelist.getEmail() != null && !this.panelist.getEmail().isEmpty()) {
					if (panelistService.existsByEmail(this.panelist.getEmail())) {
						Notification.show("Ya existe un panelista con el correo electrónico: " + this.panelist.getEmail(), 5000, Notification.Position.MIDDLE)
								.addThemeVariants(NotificationVariant.LUMO_ERROR);
						return; // Detener el proceso de guardado
					}
				}

				// Set<PanelistProperty> selectedProperties = propertiesField.getValue(); //
				// Removed
				// this.panelist.setProperties(new HashSet<>(selectedProperties)); // Removed -
				// will be handled by PanelistPropertyValue logic later

				panelistService.save(this.panelist);
				clearForm();
				refreshGrid();
				Notification.show("Datos actualizados");
				UI.getCurrent().navigate(PanelistsView.class);
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
		Optional<Long> panelistId = event.getRouteParameters().get(PANELIST_ID).map(Long::parseLong);
		if (panelistId.isPresent()) {
			Optional<Panelist> panelistFromBackend = panelistService.get(panelistId.get());
			if (panelistFromBackend.isPresent()) {
				populateForm(panelistFromBackend.get());
				editorLayoutDiv.setVisible(true);
			} else {
				Notification.show(String.format("El panelista solicitado no fue encontrado, ID = %s", panelistId.get()),
						3000, Notification.Position.BOTTOM_START);
				// when a row is selected but the data is no longer available,
				// refresh grid
				refreshGrid();
				if (editorLayoutDiv != null) { // Asegurar que no sea nulo si beforeEnter se llama muy temprano
					editorLayoutDiv.setVisible(false);
				}
				event.forwardTo(PanelistsView.class);
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
		firstName = new TextField("Nombre");
		lastName = new TextField("Apellido");
		email = new TextField("Correo Electrónico");
		phone = new TextField("Teléfono");
		source = new TextField("Fuente"); // Added source field to form
		// dateOfBirth = new DatePicker("Fecha de Nacimiento"); // Removed
		// occupation = new TextField("Ocupación"); // Removed
		lastContacted = new DatePicker("Último encuesta enviada");
		lastContacted.setReadOnly(true);
		lastContacted.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n lcI18n = new DatePicker.DatePickerI18n();
		lcI18n.setDateFormat("dd/MM/yyyy");
		lastContacted.setI18n(lcI18n);

		lastInterviewCompleted = new DatePicker("Última encuesta completa");
		lastInterviewCompleted.setReadOnly(true);
		lastInterviewCompleted.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n licI18n = new DatePicker.DatePickerI18n();
		licI18n.setDateFormat("dd/MM/yyyy");
		lastInterviewCompleted.setI18n(licI18n);

		// START: Add properties field - Removed
		// propertiesField = new MultiSelectListBox<>(); // Removed
		// propertiesField.setItems(panelistPropertyService.findAll()); // Removed
		// propertiesField.setItemLabelGenerator(PanelistProperty::getName); // Removed
		// END: Add properties field -- Removed
		gestionarPropiedadesButton = new Button("Gestionar Propiedades"); // Added
		gestionarPropiedadesButton.addClickListener(e -> {
			if (this.panelist != null) {
				createGestionarPropiedadesDialog(); // Ensure dialog is created
				// Here you would typically pass the current panelist's data to the dialog
				// or refresh the dialog's content based on the current panelist.
				// For now, just opening it.
				gestionarPropiedadesDialog.open();
			} else {
				Notification.show("Por favor, seleccione un panelista primero.", 3000, Notification.Position.MIDDLE);
			}
		});
		formLayout.add(firstName, lastName, email, phone, source, lastContacted, lastInterviewCompleted); // Added source,
																									// Removed
																									// dateOfBirth,
																									// occupation
		formLayout.add(gestionarPropiedadesButton, viewParticipatingPanelsButton, viewParticipatingSurveysButton); // Added
																													// viewParticipatingSurveysButton

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
		headerRow.getCell(grid.getColumnByKey("firstName")).setComponent(firstNameFilter);
		headerRow.getCell(grid.getColumnByKey("lastName")).setComponent(lastNameFilter);
		headerRow.getCell(grid.getColumnByKey("email")).setComponent(emailFilter);
		headerRow.getCell(grid.getColumnByKey("phone")).setComponent(phoneFilter);
		// headerRow.getCell(grid.getColumnByKey("dateOfBirth")).setComponent(dateOfBirthFilter);
		// // Removed
		// headerRow.getCell(grid.getColumnByKey("occupation")).setComponent(occupationFilter);
		// // Removed
		headerRow.getCell(grid.getColumnByKey("lastContacted")).setComponent(lastContactedFilter);
		headerRow.getCell(grid.getColumnByKey("lastInterviewCompleted")).setComponent(lastInterviewCompletedFilter);
		headerRow.getCell(grid.getColumnByKey("source")).setComponent(sourceFilter); // Added source filter to header
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void populateForm(Panelist value) {
		this.panelist = value;
		binder.readBean(this.panelist);

		if (deleteButton != null) {
			deleteButton.setEnabled(value != null && value.getId() != null);
		}
		// START: Populate properties field - Removed
		// if (value != null && value.getProperties() != null) { // Removed
		// propertiesField.setValue(value.getProperties()); // Removed
		// } else { // Removed
		// if (propertiesField != null) { // Check if initialized // Removed
		// propertiesField.clear(); // Removed
		// } // Removed
		// } // Removed
		// END: Populate properties field - Removed
		if (viewParticipatingPanelsButton != null) {
			viewParticipatingPanelsButton.setEnabled(value != null && value.getId() != null);
		}
		if (viewParticipatingSurveysButton != null) {
			viewParticipatingSurveysButton.setEnabled(value != null && value.getId() != null);
		}
	}

	private void openParticipatingSurveysDialog() {
		if (this.panelist == null || this.panelist.getId() == null) {
			Notification.show("No hay un panelista seleccionado.", 3000, Notification.Position.MIDDLE);
			return;
		}

		// Re-fetch the panelist with participations explicitly loaded
		Optional<Panelist> panelistOpt = panelistService.findByIdWithParticipations(this.panelist.getId());

		if (panelistOpt.isEmpty()) {
			Notification.show("No se pudo cargar el panelista seleccionado o sus participaciones.", 3000,
					Notification.Position.MIDDLE);
			return;
		}

		Panelist currentPanelist = panelistOpt.get();

		// Use getParticipations() which should be populated if Panelist entity and
		// service layer handle it.
		Set<uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation> participations = currentPanelist
				.getParticipations();

		if (participations == null || participations.isEmpty()) {
			Notification.show("Este panelista no tiene participaciones en encuestas.", 3000,
					Notification.Position.MIDDLE);
			return;
		}

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Participaciones en Encuestas: " + currentPanelist.getFirstName() + " "
				+ currentPanelist.getLastName());
		dialog.setWidth("80%");
		dialog.setHeight("70%");

		Grid<uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation> participationGrid = new Grid<>(
				uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation.class, false);

		// Define columns
		Grid.Column<uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation> surveyNameCol = participationGrid
				.addColumn(part -> part.getSurvey().getName()).setHeader("Nombre de encuesta").setSortable(true)
				.setKey("surveyName");
		Grid.Column<uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation> dateSentCol = participationGrid
				.addColumn(uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation::getDateSent)
				.setHeader("Fecha de envio").setSortable(true).setKey("dateSent");
		Grid.Column<uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation> dateCompletedCol = participationGrid
				.addColumn(uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation::getDateCompleted)
				.setHeader("Fecha completa").setSortable(true).setKey("dateCompleted");
		Grid.Column<uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation> completedCol = participationGrid
				.addComponentColumn(part -> {
					Checkbox checkbox = new Checkbox(part.isCompleted());
					checkbox.setReadOnly(true);
					return checkbox;
				}).setHeader("Completa").setSortable(true).setKey("completed");

		// Filters
		HeaderRow filterRow = participationGrid.appendHeaderRow();

		TextField surveyNameFilter = new TextField();
		surveyNameFilter.setPlaceholder("Filtrar...");
		filterRow.getCell(surveyNameCol).setComponent(surveyNameFilter);

		DatePicker dateSentFilter = new DatePicker();
		dateSentFilter.setPlaceholder("dd/MM/yyyy");
		dateSentFilter.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n dsfI18n = new DatePicker.DatePickerI18n();
		dsfI18n.setDateFormat("dd/MM/yyyy");
		dateSentFilter.setI18n(dsfI18n);
		filterRow.getCell(dateSentCol).setComponent(dateSentFilter);

		DatePicker dateCompletedFilter = new DatePicker();
		dateCompletedFilter.setPlaceholder("dd/MM/yyyy");
		dateCompletedFilter.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n dcfI18n = new DatePicker.DatePickerI18n();
		dcfI18n.setDateFormat("dd/MM/yyyy");
		dateCompletedFilter.setI18n(dcfI18n);
		filterRow.getCell(dateCompletedCol).setComponent(dateCompletedFilter);

		ComboBox<String> completedFilter = new ComboBox<>();
		completedFilter.setItems("Todos", "Sí", "No");
		completedFilter.setValue("Todos"); // Default to show all
		completedFilter.setPlaceholder("Filtrar...");
		filterRow.getCell(completedCol).setComponent(completedFilter);

		// DataProvider
		ListDataProvider<uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation> dataProvider = new ListDataProvider<>(
				new ArrayList<>(participations));
		participationGrid.setDataProvider(dataProvider);

		// Filter logic
		surveyNameFilter.addValueChangeListener(e -> dataProvider.refreshAll());
		dateSentFilter.addValueChangeListener(e -> dataProvider.refreshAll());
		dateCompletedFilter.addValueChangeListener(e -> dataProvider.refreshAll());
		completedFilter.addValueChangeListener(e -> dataProvider.refreshAll());

		dataProvider.addFilter(participation -> {
			boolean surveyNameMatch = true;
			if (StringUtils.isNotBlank(surveyNameFilter.getValue())) {
				surveyNameMatch = StringUtils.containsIgnoreCase(participation.getSurvey().getName(),
						surveyNameFilter.getValue());
			}

			boolean dateSentMatch = true;
			if (dateSentFilter.getValue() != null) {
				dateSentMatch = participation.getDateSent() != null
						&& participation.getDateSent().equals(dateSentFilter.getValue());
			}

			boolean dateCompletedMatch = true;
			if (dateCompletedFilter.getValue() != null) {
				dateCompletedMatch = participation.getDateCompleted() != null
						&& participation.getDateCompleted().equals(dateCompletedFilter.getValue());
			}

			boolean completedMatch = true;
			String completedValue = completedFilter.getValue();
			if (completedValue != null && !"Todos".equals(completedValue)) {
				boolean expectedState = "Sí".equals(completedValue);
				completedMatch = participation.isCompleted() == expectedState;
			}

			return surveyNameMatch && dateSentMatch && completedMatch;
		});

		dialog.add(participationGrid);
		Button closeButton = new Button("Cerrar", e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getFooter().add(closeButton);

		dialog.open();
	}

	// Changed to package-private for testing
	void createOrOpenViewPanelsDialog() {
		viewPanelsDialog = new Dialog();
		viewPanelsDialog.setHeaderTitle("Paneles en los que participa: " + (currentPanelistForPanelsDialog != null
				? currentPanelistForPanelsDialog.getFirstName() + " " + currentPanelistForPanelsDialog.getLastName()
				: ""));
		viewPanelsDialog.setWidth("1200px");
		viewPanelsDialog.setHeight("600px");

		// Initialize the field to track changes locally for this dialog instance
		modifiedPanelsInDialog = new HashSet<>();
		if (currentPanelistForPanelsDialog != null && currentPanelistForPanelsDialog.getPanels() != null) {
			modifiedPanelsInDialog.addAll(currentPanelistForPanelsDialog.getPanels());
		}

		participatingPanelsGrid = new Grid<>(Panel.class, false);
		// Fetch all panels and set them up with a ListDataProvider
		List<Panel> allPanelsList = panelService.findAll();
		ListDataProvider<Panel> allPanelsDataProvider = new ListDataProvider<>(allPanelsList);
		participatingPanelsGrid.setDataProvider(allPanelsDataProvider);
		// Add Checkbox column for participation (must be configured after data columns
		// for header row)

		Grid.Column<Panel> participationColumn = participatingPanelsGrid.addComponentColumn(panel -> {
			Checkbox checkbox = new Checkbox();
			if (currentPanelistForPanelsDialog != null && currentPanelistForPanelsDialog.getPanels() != null) {
				checkbox.setValue(currentPanelistForPanelsDialog.getPanels().contains(panel));
			} else {
				checkbox.setValue(false);
			}
			checkbox.addValueChangeListener(event -> {
				if (currentPanelistForPanelsDialog != null) {
					if (event.getValue()) {
						modifiedPanelsInDialog.add(panel);
						Notification.show(currentPanelistForPanelsDialog.getFirstName() + " participará en "
								+ panel.getName() + " (cambios pendientes)", 2000, Position.BOTTOM_START);
					} else {
						modifiedPanelsInDialog.remove(panel);
						Notification.show(currentPanelistForPanelsDialog.getFirstName() + " no participará en "
								+ panel.getName() + " (cambios pendientes)", 2000, Position.BOTTOM_START);
					}
				}
			});
			return checkbox;
		}).setHeader("Participa").setWidth("100px").setFlexGrow(0);
		// Define columns before adding header row for filters

		Grid.Column<Panel> nameColumn = participatingPanelsGrid.addColumn(Panel::getName).setHeader("Nombre")
				.setKey("name").setAutoWidth(true);
		Grid.Column<Panel> createdColumn = participatingPanelsGrid.addColumn(Panel::getCreated).setHeader("Creado")
				.setKey("created").setAutoWidth(true);
		Grid.Column<Panel> activeColumn = participatingPanelsGrid.addColumn(Panel::isActive).setHeader("Activo")
				.setKey("active").setAutoWidth(true);

		// Add filter row
		HeaderRow filterRow = participatingPanelsGrid.appendHeaderRow();

		// Name filter
		TextField nameFilterField = new TextField();
		nameFilterField.setPlaceholder("Filtrar");
		nameFilterField.setWidthFull();
		nameFilterField.addValueChangeListener(event -> allPanelsDataProvider
				.addFilter(panel -> StringUtils.containsIgnoreCase(panel.getName(), nameFilterField.getValue())));
		filterRow.getCell(nameColumn).setComponent(nameFilterField);

		// Created filter
		TextField createdFilterField = new TextField();
		createdFilterField.setPlaceholder("Filtrar");
		createdFilterField.setWidthFull();
		createdFilterField.addValueChangeListener(
				event -> allPanelsDataProvider.addFilter(panel -> panel.getCreated() != null && StringUtils
						.containsIgnoreCase(panel.getCreated().toString(), createdFilterField.getValue())));
		filterRow.getCell(createdColumn).setComponent(createdFilterField);

		// Active filter
		ComboBox<String> activeFilterField = new ComboBox<>();
		activeFilterField.setItems("Todos", "Sí", "No");
		activeFilterField.setPlaceholder("Filtrar");
		activeFilterField.setWidthFull();
		activeFilterField.addValueChangeListener(event -> {
			String value = activeFilterField.getValue();
			allPanelsDataProvider.setFilter(panel -> { // Using setFilter to replace previous active filter
				if (value == null || "Todos".equals(value)) {
					return true;
				}
				boolean isActiveSearch = "Sí".equals(value);
				return panel.isActive() == isActiveSearch;
			});
			// Need to re-apply other filters if setFilter clears them.
			// A better approach is a combined filter. Let's refine this.
		});
		// Temporary: For simplicity, this will overwrite other filters. Will be
		// improved.
		// filterRow.getCell(activeColumn).setComponent(activeFilterField);

		// Combined filter approach
		namePanelFilterDialog = new TextField();
		namePanelFilterDialog.setPlaceholder("Nombre...");
		namePanelFilterDialog.setWidthFull();
		namePanelFilterDialog.addValueChangeListener(e -> allPanelsDataProvider.refreshAll());
		filterRow.getCell(nameColumn).setComponent(namePanelFilterDialog);

		createdPanelFilterDialog = new TextField();
		createdPanelFilterDialog.setPlaceholder("Fecha...");
		createdPanelFilterDialog.setWidthFull();
		createdPanelFilterDialog.addValueChangeListener(e -> allPanelsDataProvider.refreshAll());
		filterRow.getCell(createdColumn).setComponent(createdPanelFilterDialog);

		activePanelFilterDialog = new ComboBox<>();
		activePanelFilterDialog.setItems("Todos", "Sí", "No");
		activePanelFilterDialog.setValue("Todos");
		activePanelFilterDialog.setWidthFull();
		activePanelFilterDialog.addValueChangeListener(e -> allPanelsDataProvider.refreshAll());
		filterRow.getCell(activeColumn).setComponent(activePanelFilterDialog);

		// No filter for participation column's header
		// filterRow.getCell(participationColumn).setComponent(new Div()); // Or leave
		// empty

		allPanelsDataProvider.setFilter(panel -> {
			boolean nameMatch = StringUtils.containsIgnoreCase(panel.getName(), namePanelFilterDialog.getValue());
			boolean createdMatch = panel.getCreated() != null && StringUtils
					.containsIgnoreCase(panel.getCreated().toString(), createdPanelFilterDialog.getValue());

			String activeFilterValue = activePanelFilterDialog.getValue();
			boolean activeMatch = true;
			if (activeFilterValue != null && !"Todos".equals(activeFilterValue)) {
				boolean isActiveSearch = "Sí".equals(activeFilterValue);
				activeMatch = panel.isActive() == isActiveSearch;
			}
			return nameMatch && createdMatch && activeMatch;
		});

		viewPanelsDialog.add(participatingPanelsGrid);

		savePanelsButtonDialog = new Button("Guardar");
		savePanelsButtonDialog.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		savePanelsButtonDialog.addClickListener(e -> {
			if (currentPanelistForPanelsDialog != null) {
				try {
					currentPanelistForPanelsDialog.setPanels(modifiedPanelsInDialog);
					panelistService.save(currentPanelistForPanelsDialog);
					Notification.show("Cambios en paneles guardados exitosamente.", 3000, Position.BOTTOM_START);
					viewPanelsDialog.close();
					refreshGrid(); // Refresh main grid to reflect potential changes if needed elsewhere
				} catch (ObjectOptimisticLockingFailureException exception) {
					Notification n = Notification
							.show("Error al guardar los cambios. Otro usuario modificó el registro.");
					n.setPosition(Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				} catch (Exception ex) {
					Notification.show("Error al guardar cambios en paneles: " + ex.getMessage(), 5000, Position.MIDDLE)
							.addThemeVariants(NotificationVariant.LUMO_ERROR);
				}
			}
		});

		cancelPanelsButtonDialog = new Button("Cancelar", e -> viewPanelsDialog.close());
		cancelPanelsButtonDialog.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

		viewPanelsDialog.getFooter().add(cancelPanelsButtonDialog, savePanelsButtonDialog); // Order: Cancel, Save

		viewPanelsDialog.open();
	}

	private void confirmRemovePanelFromPanelist(Panel panelToRemove) {
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setHeader("Confirmar Eliminación");
		dialog.setText("Está seguro de que desea eliminar la participación de '"
				+ currentPanelistForPanelsDialog.getFirstName() + " " + currentPanelistForPanelsDialog.getLastName()
				+ "' en el panel '" + panelToRemove.getName() + "'?");

		dialog.setConfirmText("Eliminar");
		dialog.setConfirmButtonTheme("error primary");
		dialog.setCancelText("Cancelar");

		dialog.addConfirmListener(event -> {
			// TODO: Call
			// panelistService.removePanelFromPanelist(currentPanelistForPanelsDialog.getId(),
			// panelToRemove.getId());
			// This service method will be created in a later step.
			// For now, we directly modify the collection and save the panelist.

			if (currentPanelistForPanelsDialog != null && currentPanelistForPanelsDialog.getPanels() != null) {
				currentPanelistForPanelsDialog.getPanels().remove(panelToRemove);
				try {
					panelistService.save(currentPanelistForPanelsDialog); // Persist the change
					if (participatingPanelsGrid != null) { // Refresh grid
						participatingPanelsGrid.setItems(new ArrayList<>(currentPanelistForPanelsDialog.getPanels()));
					}
					Notification.show("Participación en panel eliminada.", 3000, Notification.Position.BOTTOM_START);
				} catch (ObjectOptimisticLockingFailureException exception) {
					Notification n = Notification
							.show("Error al actualizar los datos. Otro usuario modificó el registro.");
					n.setPosition(Notification.Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				} catch (Exception e) {
					Notification.show("Error al eliminar participación: " + e.getMessage(), 5000,
							Notification.Position.MIDDLE);
				}
			}
		});
		dialog.open();
	}

	private void createGestionarPropiedadesDialog() {
		// if (gestionarPropiedadesDialog == null) { // Keep this if structure if you
		// want to create dialog only once
		// For development, it might be easier to recreate it each time to see changes,
		// or add logic to clear and repopulate its content.
		// Let's assume we recreate or clear content for now.

		gestionarPropiedadesDialog = new Dialog(); // Recreate for simplicity during dev, or clear previous content
		gestionarPropiedadesDialog.setHeaderTitle("Gestionar Propiedades del Panelista: "
				+ (this.panelist != null ? this.panelist.getFirstName() + " " + this.panelist.getLastName() : ""));
		gestionarPropiedadesDialog.setWidth("750px"); // Set a reasonable width
		gestionarPropiedadesDialog.setHeight("500px");

		final List<PanelistProperty> allProperties = panelistPropertyService.findAll();

		// Prepare data for checkbox states
		final Set<PanelistProperty> linkedProperties = new HashSet<>();
		if (this.panelist != null && this.panelist.getId() != null && this.panelist.getPropertyValues() != null) {
			linkedProperties.addAll(this.panelist.getPropertyValues().stream()
					.map(PanelistPropertyValue::getPanelistProperty).collect(Collectors.toSet()));
		}
		final Map<PanelistProperty, Checkbox> propertyCheckboxes = new HashMap<>();

		Grid<PanelistProperty> propertiesGrid = new Grid<>(PanelistProperty.class, false);

		// Add the Checkbox column as the first column
		propertiesGrid.addComponentColumn(panelistProperty -> {
			Checkbox checkbox = new Checkbox();
			checkbox.setValue(linkedProperties.contains(panelistProperty));
			propertyCheckboxes.put(panelistProperty, checkbox);
			return checkbox;
		}).setHeader("Vincular").setWidth("100px").setFlexGrow(0);

		ListDataProvider<PanelistProperty> dataProvider = new ListDataProvider<>(allProperties);
		propertiesGrid.setDataProvider(dataProvider);

		// Map to hold TextFields for each PanelistProperty ID
		final Map<Long, Component> propertyValueFields = new HashMap<>();

		// Load existing values for the current panelist
		final Map<PanelistProperty, String> existingValuesMap = new HashMap<>(); // Made final
		if (this.panelist != null && this.panelist.getId() != null) { // Ensure panelist is not new
			Set<PanelistPropertyValue> currentValues = this.panelist.getPropertyValues();
			if (currentValues != null) {
				existingValuesMap.putAll(currentValues.stream().collect(
						Collectors.toMap(PanelistPropertyValue::getPanelistProperty, PanelistPropertyValue::getValue)));
			}
		}

		Grid.Column<PanelistProperty> nameColumn = propertiesGrid.addColumn(PanelistProperty::getName)
				.setHeader("Propiedad").setKey("name").setFlexGrow(1);
		Grid.Column<PanelistProperty> typeColumn = propertiesGrid.addColumn(PanelistProperty::getType).setHeader("Tipo")
				.setKey("type").setFlexGrow(1);

		Grid.Column<PanelistProperty> valueColumn = propertiesGrid.addComponentColumn(panelistProperty -> {
			PropertyType type = panelistProperty.getType();
			String existingValue = existingValuesMap.get(panelistProperty);
			Component editorComponent;

			switch (type) {
			case FECHA:
				DatePicker datePicker = new DatePicker();
				datePicker.setPlaceholder("dd/MM/yyyy");
				datePicker.setLocale(new Locale("es", "UY"));
				DatePicker.DatePickerI18n dpI18n = new DatePicker.DatePickerI18n();
				dpI18n.setDateFormat("dd/MM/yyyy");
				datePicker.setI18n(dpI18n);
				if (existingValue != null && !existingValue.isEmpty()) {
					try {
						// Attempt to parse if it's in ISO format (yyyy-MM-dd) first
						datePicker.setValue(LocalDate.parse(existingValue));
					} catch (DateTimeParseException e) {
						// If ISO parsing fails, try dd/MM/yyyy (though data should ideally be stored in ISO)
						try {
							DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
							datePicker.setValue(LocalDate.parse(existingValue, customFormatter));
						} catch (DateTimeParseException ex) {
							// Handle or log parsing error, maybe set to null
							datePicker.setValue(null);
						}
					}
				}
				editorComponent = datePicker;
				break;
			case NUMERO:
				TextField numeroField = new TextField();
				numeroField.setPlaceholder("Valor numérico...");
				// Basic numeric validation (optional, can be enhanced with patterns)
				// numeroField.setPattern("[0-9]*");
				if (existingValue != null) {
					numeroField.setValue(existingValue);
				}
				editorComponent = numeroField;
				break;
			case CODIGO:
				ComboBox<String> comboBox = new ComboBox<>();
				comboBox.setPlaceholder("Seleccione código...");
				List<PanelistPropertyCode> codes = panelistPropertyCodeRepository
						.findByPanelistProperty(panelistProperty);
				List<String> codeValues = codes.stream().map(PanelistPropertyCode::getCode)
						.collect(Collectors.toList());
				comboBox.setItems(codeValues);
				if (existingValue != null) {
					comboBox.setValue(existingValue);
				}
				editorComponent = comboBox;
				break;
			case TEXTO:
			default:
				TextField textField = new TextField();
				textField.setPlaceholder("Valor...");
				if (existingValue != null) {
					textField.setValue(existingValue);
				}
				editorComponent = textField;
				break;
			}
			propertyValueFields.put(panelistProperty.getId(), editorComponent);
			return editorComponent;
		}).setHeader("Valor").setKey("valor").setFlexGrow(2);

		// propertiesGrid.setItems(allProperties); // DataProvider is used now
		propertiesGrid.setWidthFull();

		// Filters
		TextField nameFilter = new TextField();
		nameFilter.setPlaceholder("Filtrar por Propiedad");
		nameFilter.setWidthFull();
		nameFilter.getStyle().set("max-width", "100%");

		TextField typeFilter = new TextField();
		typeFilter.setPlaceholder("Filtrar por Tipo");
		typeFilter.setWidthFull();
		typeFilter.getStyle().set("max-width", "100%");

		TextField valueFilter = new TextField();
		valueFilter.setPlaceholder("Filtrar por Valor");
		valueFilter.setWidthFull();
		valueFilter.getStyle().set("max-width", "100%");

		HeaderRow filterHeaderRow = propertiesGrid.appendHeaderRow();
		filterHeaderRow.getCell(nameColumn).setComponent(nameFilter);
		filterHeaderRow.getCell(typeColumn).setComponent(typeFilter);
		filterHeaderRow.getCell(valueColumn).setComponent(valueFilter);

		nameFilter.addValueChangeListener(event -> dataProvider.refreshAll());
		typeFilter.addValueChangeListener(event -> dataProvider.refreshAll());
		valueFilter.addValueChangeListener(event -> dataProvider.refreshAll());

		dataProvider.addFilter(panelistProperty -> {
			String nameSearch = nameFilter.getValue().trim().toLowerCase();
			if (nameSearch.isEmpty())
				return true;
			String propertyName = panelistProperty.getName();
			return propertyName != null && propertyName.toLowerCase().contains(nameSearch);
		});
		dataProvider.addFilter(panelistProperty -> {
			String typeSearch = typeFilter.getValue().trim().toLowerCase();
			if (typeSearch.isEmpty())
				return true;
			String propertyType = panelistProperty.getType().toString();
			return propertyType != null && propertyType.toLowerCase().contains(typeSearch);
		});
		dataProvider.addFilter(panelistProperty -> {
			String valueSearch = valueFilter.getValue().trim().toLowerCase();
			if (valueSearch.isEmpty()) {
				return true;
			}
			String propertyCurrentValue = existingValuesMap.get(panelistProperty);
			return propertyCurrentValue != null && propertyCurrentValue.toLowerCase().contains(valueSearch);
		});

		gestionarPropiedadesDialog.add(propertiesGrid);

		Button saveDialogButton = new Button("Guardar", e -> {
			if (this.panelist == null || this.panelist.getId() == null) {
				Notification.show(
						"No hay un panelista seleccionado o el panelista es nuevo. Guarde el panelista primero.", 3000,
						Notification.Position.MIDDLE);
				return;
			}

			// Initialize finalPpvSet which will hold the desired state
			Set<PanelistPropertyValue> finalPpvSet = new HashSet<>();

			for (PanelistProperty prop : allProperties) {
				Checkbox checkbox = propertyCheckboxes.get(prop);
				Component valueComponent = propertyValueFields.get(prop.getId());
				String newValue = "";

				if (valueComponent instanceof TextField) {
					newValue = ((TextField) valueComponent).getValue();
				} else if (valueComponent instanceof DatePicker) {
					LocalDate dateValue = ((DatePicker) valueComponent).getValue();
					newValue = (dateValue != null) ? dateValue.toString() : "";
				} else if (valueComponent instanceof ComboBox) {
					newValue = (String) ((ComboBox<?>) valueComponent).getValue(); // Cast to ComboBox<?> before
																					// getValue
				}
				newValue = (newValue != null) ? newValue : "";

				if (checkbox != null && checkbox.getValue()) { // Checkbox is CHECKED
					Optional<PanelistPropertyValue> ppvOpt = panelistPropertyValueService
							.findByPanelistAndPanelistProperty(this.panelist, prop);
					PanelistPropertyValue ppvToProcess;
					if (ppvOpt.isPresent()) { // Exists in DB
						ppvToProcess = ppvOpt.get();
						ppvToProcess.setValue(newValue); // Update value
					} else { // Does not exist in DB, create new
						ppvToProcess = new PanelistPropertyValue();
						ppvToProcess.setPanelist(this.panelist);
						ppvToProcess.setPanelistProperty(prop);
						ppvToProcess.setValue(newValue);
					}
					finalPpvSet.add(ppvToProcess);
				} else {
					// NOT CHECKED: If it existed, it will not be added to finalPpvSet.
					// orphanRemoval=true on Panelist.propertyValues should handle deletion from DB
					// when panelistService.save() is called if it was in the original collection.
				}
			}

			// Update the panelist's collection to the final desired state
			if (this.panelist.getPropertyValues() == null) {
				this.panelist.setPropertyValues(new HashSet<>());
			}
			this.panelist.getPropertyValues().clear();
			this.panelist.getPropertyValues().addAll(finalPpvSet);

			try {
				panelistService.save(this.panelist);
				Notification.show("Propiedades guardadas para " + this.panelist.getFirstName() + " "
						+ this.panelist.getLastName(), 3000, Notification.Position.BOTTOM_START);
				gestionarPropiedadesDialog.close();
			} catch (Exception ex) {
				Notification.show("Error al guardar propiedades: " + ex.getMessage(), 5000,
						Notification.Position.MIDDLE);
				// ex.printStackTrace(); // Uncomment for debugging if needed
			}
		});
		saveDialogButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelDialogButton = new Button("Cancelar", e -> gestionarPropiedadesDialog.close());
		gestionarPropiedadesDialog.getFooter().add(cancelDialogButton, saveDialogButton);
		// } // End of if (gestionarPropiedadesDialog == null)
	}

	private void clearForm() {
		populateForm(null);
		if (editorLayoutDiv != null) { // Buena práctica verificar nulidad
			editorLayoutDiv.setVisible(false);
		}
		if (deleteButton != null) {
			deleteButton.setEnabled(false);
		}
		if (viewParticipatingPanelsButton != null) {
			viewParticipatingPanelsButton.setEnabled(false);
		}
		if (viewParticipatingSurveysButton != null) { // Added
			viewParticipatingSurveysButton.setEnabled(false); // Added
		} // Added
			// Explicitly clear propertiesField, ensure it's initialized - Removed
			// if (propertiesField != null) { // Removed
			// propertiesField.clear(); // Removed
			// } // Removed
	}

	private void onDeleteClicked() {
		if (this.panelist == null || this.panelist.getId() == null) {
			Notification.show("No hay panelista seleccionado para eliminar.", 3000, Notification.Position.MIDDLE);
			return;
		}

		com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
		dialog.setHeader("Confirmar Eliminación");
		dialog.setText("¿Está seguro de que desea eliminar el panelista '" + this.panelist.getFirstName() + " "
				+ this.panelist.getLastName() + "'?");

		dialog.setConfirmText("Eliminar");
		dialog.setConfirmButtonTheme("error primary");
		dialog.setCancelText("Cancelar");

		dialog.addConfirmListener(event -> {
			try {
				panelistService.delete(this.panelist.getId());
				clearForm();
				refreshGrid();
				Notification.show("Panelista eliminado correctamente.", 3000, Notification.Position.BOTTOM_START);
				UI.getCurrent().navigate(PanelistsView.class);
			} catch (org.springframework.dao.DataIntegrityViolationException ex) {
				Notification.show(
						"No se puede eliminar el panelista. Es posible que esté siendo referenciado por otras entidades.",
						5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (Exception ex) {
				Notification.show("Ocurrió un error al intentar eliminar el panelista: " + ex.getMessage(), 5000,
						Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
		dialog.open();
	}

	private void openPanelistResultsDialog(Map<PanelistProperty, Object> filterCriteria) {
		// Crear y abrir el diálogo de resultados de panelistas.
		PanelistResultsDialog resultsDialog = new PanelistResultsDialog(panelistService, filterCriteria);
		resultsDialog.open();
	}
}
