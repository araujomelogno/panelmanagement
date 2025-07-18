package uy.com.equipos.panelmanagement.views.panels;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.Key; // Added for keyboard shortcut
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
// import com.vaadin.flow.component.html.H2; // Unused
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
// import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment; // Unused in this class
// import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode; // Unused
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.PermitAll;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCodeRepository; // Corrected path
import uy.com.equipos.panelmanagement.services.PanelService;
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;
import uy.com.equipos.panelmanagement.services.PanelistService; // Added

@PageTitle("Paneles")
@Route("/:panelID?/:action?(edit)")
@Menu(order = 0, icon = LineAwesomeIconUrl.COLUMNS_SOLID)
@RouteAlias("")
@PermitAll
@Uses(Icon.class)
public class PanelsView extends Div implements BeforeEnterObserver {

	private final String PANEL_ID = "panelID";
	private final String PANEL_EDIT_ROUTE_TEMPLATE = "/%s/edit";

	private final Grid<Panel> grid = new Grid<>(Panel.class, false);
	private Div editorLayoutDiv; // Declarado como miembro de la clase

	// Filtros de columna
	private TextField nameFilter = new TextField();
	private DatePicker createdFilter = new DatePicker();
	private ComboBox<String> activeFilter = new ComboBox<>();

	private TextField name;
	private DatePicker created;
	private Checkbox active;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button addPanelistsButton;
	private Button viewPanelistsButton; // Added
	private Button nuevoPanelButton;

	private final BeanValidationBinder<Panel> binder;

	private Panel panel;

	private final PanelService panelService;
	private final PanelistPropertyService panelistPropertyService;
	private final PanelistPropertyCodeRepository panelistPropertyCodeRepository;
	private final PanelistService panelistService; // Added

	public PanelsView(PanelService panelService, PanelistPropertyService panelistPropertyService,
			PanelistPropertyCodeRepository panelistPropertyCodeRepository, PanelistService panelistService) { // Added
		this.panelService = panelService;
		this.panelistPropertyService = panelistPropertyService;
		this.panelistPropertyCodeRepository = panelistPropertyCodeRepository;
		this.panelistService = panelistService; // Added
		addClassNames("panels-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		addPanelistsButton = new Button("Agregar Panelistas");
		addPanelistsButton.setEnabled(false);
		addPanelistsButton.addClickListener(e -> {
			if (this.panel != null && this.panel.getId() != null) {
				// Guardar una referencia al diálogo de filtro para poder cerrarlo después.
				PanelistPropertyFilterDialog filterDialog = new PanelistPropertyFilterDialog(panelistPropertyService,
						panelistPropertyCodeRepository, this.panelService, // globalPanelService
						panelistService, // panelistService
						this.panel, // currentPanel
						filterCriteria -> { // SearchListener implementation
							// Este es el comportamiento que PanelistPropertyFilterDialog tenía antes
							// internamente.
							// Ahora lo llamamos desde el listener.
							PanelistSelectionDialog selectionDialog = new PanelistSelectionDialog(this.panelService, // PanelService
																														// for
																														// Panel
									this.panelistService, // PanelistService for Panelist
									this.panel, // currentPanel
									filterCriteria, // los criterios del primer diálogo
									null // ownerDialog es null, ya que el primer diálogo se cierra solo.
							);
							selectionDialog.open();
							// PanelistPropertyFilterDialog se cierra a sí mismo después de invocar el
							// SearchListener,
							// por lo que no necesitamos cerrar filterDialog aquí explícitamente.
						});
				filterDialog.open();

			} else {
				Notification.show("Seleccione un panel primero.", 3000, Notification.Position.MIDDLE);
			}
		});

		viewPanelistsButton = new Button("Ver Panelistas");
		viewPanelistsButton.setEnabled(false);
		viewPanelistsButton.addClickListener(e -> {
			if (this.panel != null && this.panel.getId() != null) {
				showPanelistsDialog();
			} else {
				Notification.show("Seleccione un panel primero.", 3000, Notification.Position.MIDDLE);
			}
		});

		// Configurar columnas del Grid PRIMERO
		grid.addColumn(Panel::getName).setHeader("Nombre").setKey("name").setAutoWidth(true).setSortable(true);
		;
		grid.addColumn(Panel::getCreated).setHeader("Creado").setKey("created").setAutoWidth(true).setSortable(true);
		;
		LitRenderer<Panel> activeRenderer = LitRenderer.<Panel>of(
				"<vaadin-icon icon='vaadin:${item.icon}' style='width: var(--lumo-icon-size-s); height: var(--lumo-icon-size-s); color: ${item.color};'></vaadin-icon>")
				.withProperty("icon", panelItem -> panelItem.isActive() ? "check" : "minus")
				.withProperty("color", panelItem -> panelItem.isActive() ? "var(--lumo-primary-text-color)"
						: "var(--lumo-disabled-text-color)");
		grid.addColumn(activeRenderer).setHeader("Activo").setKey("active").setAutoWidth(true);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// Create UI - SplitLayout
		SplitLayout splitLayout = new SplitLayout();
		// createGridLayout ahora puede acceder a las keys de las columnas de forma
		// segura
		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);
		// editorLayoutDiv.setVisible(false); // Se maneja después de add(mainLayout)

		nuevoPanelButton = new Button("Nuevo Panel");
		nuevoPanelButton.getStyle().set("margin-left", "18px");

		VerticalLayout mainLayout = new VerticalLayout(nuevoPanelButton, splitLayout);
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);

		add(mainLayout);
		if (editorLayoutDiv != null) {
			editorLayoutDiv.setVisible(false);
		}

		// Listener para el botón "Nuevo Panel"
		nuevoPanelButton.addClickListener(click -> {
			grid.asSingleSelect().clear();
			populateForm(new Panel());
			if (editorLayoutDiv != null) {
				editorLayoutDiv.setVisible(true);
			}
			name.focus();
		});

		// Configurar placeholders para filtros (ya deberían estar inicializados como
		// miembros de clase)
		nameFilter.setPlaceholder("Filtrar por Nombre");

		createdFilter.setPlaceholder("dd/MM/yyyy");
		createdFilter.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n cfI18n = new DatePicker.DatePickerI18n();
		cfI18n.setDateFormat("dd/MM/yyyy");
		createdFilter.setI18n(cfI18n);

		activeFilter.setPlaceholder("Filtrar por Estado");
		activeFilter.setItems("Todos", "Activo", "Inactivo"); // Estos ya están en español o son universales
		activeFilter.setValue("Todos");

		// Añadir listeners para refrescar el grid cuando cambian los filtros
		// Estos listeners acceden a 'grid', que ya está inicializado.
		nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		createdFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		activeFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

		// Configurar el DataProvider del Grid
		// Esto necesita que los filtros (nameFilter, etc.) estén disponibles.
		grid.setItems(query -> {
			String nameVal = nameFilter.getValue();
			LocalDate createdVal = createdFilter.getValue();
			String activeString = activeFilter.getValue();
			Boolean activeBoolean = null;
			if ("Activo".equals(activeString)) {
				activeBoolean = true;
			} else if ("Inactivo".equals(activeString)) {
				activeBoolean = false;
			}
			return panelService
					.list(VaadinSpringDataHelpers.toSpringPageRequest(query), nameVal, createdVal, activeBoolean)
					.stream();
		});

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(PANEL_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm(); // clearForm ahora también oculta el editor
				// editorLayoutDiv.setVisible(false); // Ya se hace en clearForm
				UI.getCurrent().navigate(PanelsView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(Panel.class);

		// Bind fields. This is where you'd define e.g. validation rules
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.panel == null) {
					this.panel = new Panel();
				}
				binder.writeBean(this.panel);
				panelService.save(this.panel);
				Notification.show("Datos guardados");
				// clearForm(); // Keep form open
				// refreshGrid(); // Keep form data, refresh grid in background if necessary or let user do it manually
				grid.getDataProvider().refreshItem(this.panel, true); // Refresh only the updated/created item in the grid
                                populateForm(this.panel); // Re-populate to ensure button states are correct
				// UI.getCurrent().navigate(PanelsView.class); // Keep user on the edit view
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
		Optional<Long> panelId = event.getRouteParameters().get(PANEL_ID).map(Long::parseLong);
		if (panelId.isPresent()) {
			Optional<Panel> panelFromBackend = panelService.get(panelId.get());
			if (panelFromBackend.isPresent()) {
				populateForm(panelFromBackend.get());
				editorLayoutDiv.setVisible(true);
			} else {
				Notification.show(String.format("El panel solicitado no fue encontrado, ID = %s", panelId.get()), 3000,
						Notification.Position.BOTTOM_START);
				// when a row is selected but the data is no longer available,
				// refresh grid
				refreshGrid(); // Esto podría llamar a clearForm indirectamente si deselecciona
				editorLayoutDiv.setVisible(false); // Asegurar que esté oculto
				event.forwardTo(PanelsView.class);
			}
		} else {
			// Si no hay panelId, asegurar que el formulario esté limpio y oculto
			clearForm();
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
		created = new DatePicker("Fecha de Creación");
		created.setLocale(new Locale("es", "UY"));
		DatePicker.DatePickerI18n cI18n = new DatePicker.DatePickerI18n();
		cI18n.setDateFormat("dd/MM/yyyy");
		created.setI18n(cI18n);
		active = new Checkbox("Activo");
		formLayout.add(name, created, active, addPanelistsButton, viewPanelistsButton);

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
		headerRow.getCell(grid.getColumnByKey("created")).setComponent(createdFilter);
		headerRow.getCell(grid.getColumnByKey("active")).setComponent(activeFilter);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void populateForm(Panel value) {
		this.panel = value;
		binder.readBean(this.panel);

		if (deleteButton != null) {
			deleteButton.setEnabled(value != null && value.getId() != null);
		}
		if (addPanelistsButton != null) {
			addPanelistsButton.setEnabled(value != null && value.getId() != null);
		}
		if (viewPanelistsButton != null) { // Added
			viewPanelistsButton.setEnabled(value != null && value.getId() != null);
		}
	}

	private void clearForm() {
		populateForm(null);
		editorLayoutDiv.setVisible(false); // Ocultar el editor al limpiar el formulario
		if (deleteButton != null) {
			deleteButton.setEnabled(false);
		}
		if (addPanelistsButton != null) {
			addPanelistsButton.setEnabled(false);
		}
		if (viewPanelistsButton != null) { // Added
			viewPanelistsButton.setEnabled(false);
		}
	}

	private void onDeleteClicked() {
		if (this.panel == null || this.panel.getId() == null) {
			Notification.show("No hay panel seleccionado para eliminar.", 3000, Notification.Position.MIDDLE);
			return;
		}

		com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
		dialog.setHeader("Confirmar Eliminación");
		dialog.setText("¿Está seguro de que desea eliminar el panel '" + this.panel.getName() + "'?");

		dialog.setConfirmText("Eliminar");
		dialog.setConfirmButtonTheme("error primary");
		dialog.setCancelText("Cancelar");

		dialog.addConfirmListener(event -> {
			try {
				panelService.delete(this.panel.getId());
				clearForm();
				refreshGrid();
				Notification.show("Panel eliminado correctamente.", 3000, Notification.Position.BOTTOM_START);
				UI.getCurrent().navigate(PanelsView.class);
			} catch (org.springframework.dao.DataIntegrityViolationException ex) {
				Notification.show(
						"No se puede eliminar el panel. Es posible que esté siendo referenciado por otras entidades.",
						5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (Exception ex) {
				Notification.show("Ocurrió un error al intentar eliminar el panel: " + ex.getMessage(), 5000,
						Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
		dialog.open();
	}

	private void showPanelistsDialog() {
		if (this.panel != null && this.panel.getId() != null) {
			ViewPanelistsDialog dialog = new ViewPanelistsDialog(this.panel, this.panelService, this.panelistService); // Added
																														// panelistService
			dialog.open();
		} else {
			Notification.show("No hay un panel seleccionado.", 3000, Notification.Position.MIDDLE);
		}
	}
	// Cambio trivial para republicar en nueva rama
}
