package uy.com.equipos.panelmanagement.views.panels;

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
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
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
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog; // Added import
import org.springframework.dao.DataIntegrityViolationException; // Added import
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.services.PanelService;

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
	private Button nuevoPanelButton;

	private final BeanValidationBinder<Panel> binder;

	private Panel panel;

	private final PanelService panelService;

	public PanelsView(PanelService panelService) {
		this.panelService = panelService;
		addClassNames("panels-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		// Configurar columnas del Grid PRIMERO
		grid.addColumn(Panel::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
		grid.addColumn(Panel::getCreated).setHeader("Creado").setKey("created").setAutoWidth(true);
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
		createdFilter.setPlaceholder("Filtrar por Fecha de Creación");
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
				clearForm();
				refreshGrid();
				Notification.show("Datos actualizados");
				UI.getCurrent().navigate(PanelsView.class);
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
		active = new Checkbox("Activo");
		formLayout.add(name, created, active);

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
	}

	private void clearForm() {
		populateForm(null);
		editorLayoutDiv.setVisible(false); // Ocultar el editor al limpiar el formulario
		if (deleteButton != null) {
			deleteButton.setEnabled(false);
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
				Notification.show("No se puede eliminar el panel. Es posible que esté siendo referenciado por otras entidades.", 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (Exception ex) {
				Notification.show("Ocurrió un error al intentar eliminar el panel: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
		dialog.open();
	}
	// Cambio trivial para republicar en nueva rama
}
