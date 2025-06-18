package uy.com.equipos.panelmanagement.views.surveys;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import jakarta.annotation.security.PermitAll;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.services.SurveyService;

@PageTitle("Encuestas")
@Route("surveys/:surveyID?/:action?(edit)")
@Menu(order = 3, icon = LineAwesomeIconUrl.QUESTION_CIRCLE_SOLID)
@PermitAll
public class SurveysView extends Div implements BeforeEnterObserver {

	private final String SURVEY_ID = "surveyID";
	private final String SURVEY_EDIT_ROUTE_TEMPLATE = "surveys/%s/edit";

	private final Grid<Survey> grid = new Grid<>(Survey.class, false);
	private Div editorLayoutDiv; // Declarado como miembro de la clase

	// Campos de filtro
	private TextField nameFilter = new TextField();
	private DatePicker initDateFilter = new DatePicker();
	private TextField linkFilter = new TextField();

	private TextField name;
	private DatePicker initDate;
	private TextField link;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button nuevaEncuestaButton;

	private final BeanValidationBinder<Survey> binder;

	private Survey survey;

	private final SurveyService surveyService;

	public SurveysView(SurveyService surveyService) {
		this.surveyService = surveyService;
		addClassNames("surveys-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

		// Configurar columnas del Grid PRIMERO
		grid.addColumn(Survey::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
		grid.addColumn(Survey::getInitDate).setHeader("Fecha de Inicio").setKey("initDate").setAutoWidth(true);
		grid.addColumn(Survey::getLink).setHeader("Enlace").setKey("link").setAutoWidth(true);
		grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		// Create UI - SplitLayout
		SplitLayout splitLayout = new SplitLayout();
		// createGridLayout ahora puede acceder a las keys de las columnas de forma
		// segura
		createGridLayout(splitLayout);
		createEditorLayout(splitLayout);
		// editorLayoutDiv.setVisible(false); // Se maneja después de add(mainLayout)

		nuevaEncuestaButton = new Button("Nueva Encuesta");
		nuevaEncuestaButton.getStyle().set("margin-left", "18px");  

		VerticalLayout mainLayout = new VerticalLayout(nuevaEncuestaButton, splitLayout);
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(false);

		add(mainLayout);
		if (editorLayoutDiv != null) {
			editorLayoutDiv.setVisible(false);
		}

		// Listener para el botón "Nueva Encuesta"
		nuevaEncuestaButton.addClickListener(click -> {
			grid.asSingleSelect().clear();
			populateForm(new Survey());
			if (editorLayoutDiv != null) {
				editorLayoutDiv.setVisible(true);
			}
			if (name != null) {
				name.focus();
			}
		});

		// Configurar placeholders para filtros
		nameFilter.setPlaceholder("Filtrar por Nombre");
		initDateFilter.setPlaceholder("Filtrar por Fecha de Inicio");
		linkFilter.setPlaceholder("Filtrar por Enlace");

		// Añadir listeners para refrescar el grid
		nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		initDateFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		linkFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

		// Configurar el DataProvider del Grid
		grid.setItems(query -> {
			String nameVal = nameFilter.getValue();
			LocalDate initDateVal = initDateFilter.getValue();
			String linkVal = linkFilter.getValue();

			return surveyService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), nameVal, initDateVal, linkVal)
					.stream();
		});

		// when a row is selected or deselected, populate form
		grid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				editorLayoutDiv.setVisible(true);
				UI.getCurrent().navigate(String.format(SURVEY_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
			} else {
				clearForm(); // clearForm ahora también oculta el editor
				UI.getCurrent().navigate(SurveysView.class);
			}
		});

		// Configure Form
		binder = new BeanValidationBinder<>(Survey.class);

		// Bind fields. This is where you'd define e.g. validation rules
		binder.bindInstanceFields(this);

		cancel.addClickListener(e -> {
			clearForm();
			refreshGrid();
		});

		save.addClickListener(e -> {
			try {
				if (this.survey == null) {
					this.survey = new Survey();
				}
				binder.writeBean(this.survey);
				surveyService.save(this.survey);
				clearForm();
				refreshGrid();
				Notification.show("Datos actualizados");
				UI.getCurrent().navigate(SurveysView.class);
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
		Optional<Long> surveyId = event.getRouteParameters().get(SURVEY_ID).map(Long::parseLong);
		if (surveyId.isPresent()) {
			Optional<Survey> surveyFromBackend = surveyService.get(surveyId.get());
			if (surveyFromBackend.isPresent()) {
				populateForm(surveyFromBackend.get());
				editorLayoutDiv.setVisible(true);
			} else {
				Notification.show(String.format("La encuesta solicitada no fue encontrada, ID = %s", surveyId.get()),
						3000, Notification.Position.BOTTOM_START);
				// when a row is selected but the data is no longer available,
				// refresh grid
				refreshGrid();
				if (editorLayoutDiv != null) {
					editorLayoutDiv.setVisible(false);
				}
				event.forwardTo(SurveysView.class);
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
		initDate = new DatePicker("Fecha de Inicio");
		link = new TextField("Enlace");
		formLayout.add(name, initDate, link);

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
		headerRow.getCell(grid.getColumnByKey("initDate")).setComponent(initDateFilter);
		headerRow.getCell(grid.getColumnByKey("link")).setComponent(linkFilter);
	}

	private void refreshGrid() {
		grid.select(null);
		grid.getDataProvider().refreshAll();
	}

	private void populateForm(Survey value) {
		this.survey = value;
		binder.readBean(this.survey);

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
		if (this.survey == null || this.survey.getId() == null) {
			Notification.show("No hay encuesta seleccionada para eliminar.", 3000, Notification.Position.MIDDLE);
			return;
		}

		com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
		dialog.setHeader("Confirmar Eliminación");
		dialog.setText("¿Está seguro de que desea eliminar la encuesta '" + this.survey.getName() + "'?");
		
		dialog.setConfirmText("Eliminar");
		dialog.setConfirmButtonTheme("error primary");
		dialog.setCancelText("Cancelar");

		dialog.addConfirmListener(event -> {
			try {
				surveyService.delete(this.survey.getId());
				clearForm();
				refreshGrid();
				Notification.show("Encuesta eliminada correctamente.", 3000, Notification.Position.BOTTOM_START);
				UI.getCurrent().navigate(SurveysView.class);
			} catch (org.springframework.dao.DataIntegrityViolationException ex) {
				Notification.show("No se puede eliminar la encuesta. Es posible que esté siendo referenciada por otras entidades.", 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			} catch (Exception ex) {
				Notification.show("Ocurrió un error al intentar eliminar la encuesta: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
					.addThemeVariants(NotificationVariant.LUMO_ERROR);
			}
		});
		dialog.open();
	}
}
