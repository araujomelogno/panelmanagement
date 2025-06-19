package uy.com.equipos.panelmanagement.views.panelists;

import com.vaadin.flow.component.Key; // Added for keyboard shortcut
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid; // Already present, but good to confirm
import com.vaadin.flow.component.grid.GridVariant;
// import com.vaadin.flow.component.grid.HeaderRow; // Will be used for filter row
import com.vaadin.flow.component.checkbox.Checkbox; // Added for checkbox column
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
import com.vaadin.flow.component.textfield.TextField; // Likely already present
import com.vaadin.flow.data.provider.ListDataProvider; // Added for filtering
import com.vaadin.flow.component.grid.HeaderRow; // Added for filtering
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
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.util.HashMap; // Added
import java.util.HashSet;
import java.util.List; // Make sure this is present
import java.util.Map; // Added
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors; // Added/Uncommented
import java.util.Collections; // Added for Collections.emptySet()
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyValue; // Added
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;
import uy.com.equipos.panelmanagement.services.PanelistPropertyValueService;
import uy.com.equipos.panelmanagement.services.PanelistService;

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
	private DatePicker lastInterviewedFilter = new DatePicker();

	private TextField firstName;
	private TextField lastName;
	private TextField email;
	private TextField phone;
	// private DatePicker dateOfBirth; // Removed
	// private TextField occupation; // Removed
	private DatePicker lastContacted;
	private DatePicker lastInterviewed;
	// private MultiSelectListBox<PanelistProperty> propertiesField; // Removed
	private Button gestionarPropiedadesButton; // Added

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button nuevoPanelistaButton;
	// private Button gestionarPropiedadesButton; // Removed duplicate declaration

	private final BeanValidationBinder<Panelist> binder;

	private Panelist panelist;

	private final PanelistService panelistService;
	private final PanelistPropertyService panelistPropertyService; // Re-added
    private final PanelistPropertyValueService panelistPropertyValueService; // Added new
    private Dialog gestionarPropiedadesDialog; // Add new

	public PanelistsView(PanelistService panelistService, PanelistPropertyService panelistPropertyService, PanelistPropertyValueService panelistPropertyValueService) {
		this.panelistService = panelistService;
		this.panelistPropertyService = panelistPropertyService; // Re-added
        this.panelistPropertyValueService = panelistPropertyValueService; // Added new
		addClassNames("panelists-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		// Configurar columnas del Grid PRIMERO
		grid.addColumn(Panelist::getFirstName).setHeader("Nombre").setKey("firstName").setAutoWidth(true);
		grid.addColumn(Panelist::getLastName).setHeader("Apellido").setKey("lastName").setAutoWidth(true);
		grid.addColumn(Panelist::getEmail).setHeader("Correo Electrónico").setKey("email").setAutoWidth(true);
		grid.addColumn(Panelist::getPhone).setHeader("Teléfono").setKey("phone").setAutoWidth(true);
		// grid.addColumn(Panelist::getDateOfBirth).setHeader("Fecha de Nacimiento").setKey("dateOfBirth")
		// .setAutoWidth(true); // Removed
		// grid.addColumn(Panelist::getOccupation).setHeader("Ocupación").setKey("occupation").setAutoWidth(true); // Removed
		grid.addColumn(Panelist::getLastContacted).setHeader("Último Contacto").setKey("lastContacted")
				.setAutoWidth(true);
		grid.addColumn(Panelist::getLastInterviewed).setHeader("Última Entrevista").setKey("lastInterviewed")
				.setAutoWidth(true);
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
		VerticalLayout mainLayout = new VerticalLayout(nuevoPanelistaButton, splitLayout);
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
		// dateOfBirthFilter.setPlaceholder("Filtrar por Fecha de Nacimiento"); // Removed
		// occupationFilter.setPlaceholder("Filtrar por Ocupación"); // Removed
		lastContactedFilter.setPlaceholder("Filtrar por Último Contacto");
		lastInterviewedFilter.setPlaceholder("Filtrar por Última Entrevista");

		// Añadir listeners para refrescar el grid
		firstNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		lastNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		emailFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		phoneFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		// dateOfBirthFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll()); // Removed
		// occupationFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll()); // Removed
		lastContactedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		lastInterviewedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

		// Configurar el DataProvider del Grid
		grid.setItems(query -> {
			String firstNameVal = firstNameFilter.getValue();
			String lastNameVal = lastNameFilter.getValue();
			String emailVal = emailFilter.getValue();
			String phoneVal = phoneFilter.getValue();
			// LocalDate dateOfBirthVal = dateOfBirthFilter.getValue(); // Removed
			// String occupationVal = occupationFilter.getValue(); // Removed
			LocalDate lastContactedVal = lastContactedFilter.getValue();
			LocalDate lastInterviewedVal = lastInterviewedFilter.getValue();

			// Adjust the call to panelistService.list to exclude dateOfBirthVal and occupationVal
			// This assumes the service method will be updated accordingly in a later step.
			// For now, we'll pass null or an equivalent that the current service method might expect,
			// or if the service method is overloaded or flexible.
			// Based on the current plan, the service method signature would change.
			// Let's assume the service method will be updated to:
			// list(Pageable, String, String, String, String, LocalDate, LocalDate)
			return panelistService.list(VaadinSpringDataHelpers.toSpringPageRequest(query),
										firstNameVal, lastNameVal, emailVal, phoneVal, 
										lastContactedVal, lastInterviewedVal).stream();
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
				binder.writeBean(this.panelist);

				// Set<PanelistProperty> selectedProperties = propertiesField.getValue(); // Removed
				// this.panelist.setProperties(new HashSet<>(selectedProperties)); // Removed - will be handled by PanelistPropertyValue logic later

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
		// dateOfBirth = new DatePicker("Fecha de Nacimiento"); // Removed
		// occupation = new TextField("Ocupación"); // Removed
		lastContacted = new DatePicker("Último Contacto");
		lastInterviewed = new DatePicker("Última Entrevista");
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
		formLayout.add(firstName, lastName, email, phone, lastContacted, lastInterviewed); // Removed dateOfBirth, occupation
		formLayout.add(gestionarPropiedadesButton); // Added button here

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
		// headerRow.getCell(grid.getColumnByKey("dateOfBirth")).setComponent(dateOfBirthFilter); // Removed
		// headerRow.getCell(grid.getColumnByKey("occupation")).setComponent(occupationFilter); // Removed
		headerRow.getCell(grid.getColumnByKey("lastContacted")).setComponent(lastContactedFilter);
		headerRow.getCell(grid.getColumnByKey("lastInterviewed")).setComponent(lastInterviewedFilter);
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
	}

	private void createGestionarPropiedadesDialog() {
        // if (gestionarPropiedadesDialog == null) { // Keep this if structure if you want to create dialog only once
        // For development, it might be easier to recreate it each time to see changes,
        // or add logic to clear and repopulate its content.
        // Let's assume we recreate or clear content for now.

        gestionarPropiedadesDialog = new Dialog(); // Recreate for simplicity during dev, or clear previous content
        gestionarPropiedadesDialog.setHeaderTitle("Gestionar Propiedades del Panelista: " +
            (this.panelist != null ? this.panelist.getFirstName() + " " + this.panelist.getLastName() : ""));
        gestionarPropiedadesDialog.setWidth("750px"); // Set a reasonable width
        gestionarPropiedadesDialog.setHeight("500px");

        final List<PanelistProperty> allProperties = panelistPropertyService.findAll();

        // Prepare data for checkbox states
        final Set<PanelistProperty> linkedProperties = new HashSet<>();
        if (this.panelist != null && this.panelist.getId() != null && this.panelist.getPropertyValues() != null) {
            linkedProperties.addAll(this.panelist.getPropertyValues().stream()
                .map(PanelistPropertyValue::getPanelistProperty)
                .collect(Collectors.toSet()));
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
        final Map<Long, TextField> propertyValueFields = new HashMap<>();

        // Load existing values for the current panelist
        final Map<PanelistProperty, String> existingValuesMap = new HashMap<>(); // Made final
        if (this.panelist != null && this.panelist.getId() != null) { // Ensure panelist is not new
            Set<PanelistPropertyValue> currentValues = this.panelist.getPropertyValues();
            if (currentValues != null) {
                existingValuesMap.putAll(currentValues.stream()
                    .collect(Collectors.toMap(PanelistPropertyValue::getPanelistProperty, PanelistPropertyValue::getValue)));
            }
        }

        Grid.Column<PanelistProperty> nameColumn = propertiesGrid.addColumn(PanelistProperty::getName).setHeader("Propiedad").setKey("name").setFlexGrow(1);
        Grid.Column<PanelistProperty> typeColumn = propertiesGrid.addColumn(PanelistProperty::getType).setHeader("Tipo").setKey("type").setFlexGrow(1);

        Grid.Column<PanelistProperty> valueColumn = propertiesGrid.addComponentColumn(panelistProperty -> {
            TextField valueField = new TextField();
            valueField.setPlaceholder("Valor...");
            String existingValue = existingValuesMap.get(panelistProperty);
            if (existingValue != null) {
                valueField.setValue(existingValue);
            }
            propertyValueFields.put(panelistProperty.getId(), valueField);
            return valueField;
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
            if (nameSearch.isEmpty()) return true;
            String propertyName = panelistProperty.getName();
            return propertyName != null && propertyName.toLowerCase().contains(nameSearch);
        });
        dataProvider.addFilter(panelistProperty -> {
            String typeSearch = typeFilter.getValue().trim().toLowerCase();
            if (typeSearch.isEmpty()) return true;
            String propertyType = panelistProperty.getType();
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
                Notification.show("No hay un panelista seleccionado o el panelista es nuevo. Guarde el panelista primero.", 3000, Notification.Position.MIDDLE);
                return;
            }

            // Initialize finalPpvSet which will hold the desired state
            Set<PanelistPropertyValue> finalPpvSet = new HashSet<>();

            for (PanelistProperty prop : allProperties) {
                Checkbox checkbox = propertyCheckboxes.get(prop);
                TextField valueField = propertyValueFields.get(prop.getId()); // Keyed by PanelistProperty.getId()
                String newValue = (valueField != null && valueField.getValue() != null) ? valueField.getValue() : "";

                if (checkbox != null && checkbox.getValue()) { // Checkbox is CHECKED
                    Optional<PanelistPropertyValue> ppvOpt = panelistPropertyValueService.findByPanelistAndPanelistProperty(this.panelist, prop);
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
                Notification.show("Propiedades guardadas para " + this.panelist.getFirstName() + " " + this.panelist.getLastName(), 3000, Notification.Position.BOTTOM_START);
                gestionarPropiedadesDialog.close();
            } catch (Exception ex) {
                Notification.show("Error al guardar propiedades: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
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
		dialog.setText("¿Está seguro de que desea eliminar el panelista '" + this.panelist.getFirstName() + " " + this.panelist.getLastName() + "'?");
		
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
				Notification.show("No se puede eliminar el panelista. Es posible que esté siendo referenciado por otras entidades.", 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (Exception ex) {
				Notification.show("Ocurrió un error al intentar eliminar el panelista: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
		dialog.open();
	}
}
