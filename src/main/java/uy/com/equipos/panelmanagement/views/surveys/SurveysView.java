package uy.com.equipos.panelmanagement.views.surveys;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.component.Key; // Added for keyboard shortcut
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.textfield.IntegerField;
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
import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.MessageTask;
import uy.com.equipos.panelmanagement.data.MessageTaskStatus;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.Panelist;
// import uy.com.equipos.panelmanagement.data.Panelist; // Ya no se usa directamente aquí
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation; // Nueva importación
import uy.com.equipos.panelmanagement.data.Tool;
import uy.com.equipos.panelmanagement.services.MessageTaskService; // Nueva importación
import uy.com.equipos.panelmanagement.services.PanelService;
import uy.com.equipos.panelmanagement.services.SurveyPanelistParticipationService; // Nueva importación
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
	private ComboBox<Tool> toolFilter = new ComboBox<>();

	private TextField name;
	private DatePicker initDate;
	private TextField link;
	private ComboBox<Tool> tool;

	private final Button cancel = new Button("Cancelar");
	private final Button save = new Button("Guardar");
	private Button deleteButton; // Add this with other button declarations
	private Button nuevaEncuestaButton;
    private Button viewParticipantsButton;
    private Button sortearPanelistasButton; // New button for drawing panelists
    private Button sendSurveysButton; // New button for sending surveys
    private Button sendReminderButton; // New button for sending reminders

	private final BeanValidationBinder<Survey> binder;

	private Survey survey;

	private final SurveyService surveyService;
    private final SurveyPanelistParticipationService participationService; // Nuevo servicio
    private final PanelService panelService; // Service for Panel entities
    private final MessageTaskService messageTaskService; // Service for MessageTask entities

	public SurveysView(SurveyService surveyService, 
                         SurveyPanelistParticipationService participationService, 
                         PanelService panelService,
                         MessageTaskService messageTaskService) {
		this.surveyService = surveyService;
        this.participationService = participationService; // Inyectar nuevo servicio
        this.panelService = panelService; // Inyectar PanelService
        this.messageTaskService = messageTaskService; // Inyectar MessageTaskService
		addClassNames("surveys-view");

		// Initialize deleteButton EARLIER
		deleteButton = new Button("Eliminar");
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.addClickListener(e -> onDeleteClicked());

        viewParticipantsButton = new Button("Ver participantes");
        viewParticipantsButton.addClickListener(e -> openParticipantsDialog());

        sortearPanelistasButton = new Button("Sortear participantes");
        sortearPanelistasButton.addClickListener(e -> openSortearPanelistasDialog());

        sendSurveysButton = new Button("Enviar encuestas");
        sendSurveysButton.addClickListener(e -> sendSurveysAction());

        sendReminderButton = new Button("Enviar recordatorio");
        sendReminderButton.addClickListener(e -> sendReminderAction());
        
		// Configurar columnas del Grid PRIMERO
		grid.addColumn(Survey::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
		grid.addColumn(Survey::getInitDate).setHeader("Fecha de Inicio").setKey("initDate").setAutoWidth(true);
		grid.addColumn(Survey::getLink).setHeader("Enlace").setKey("link").setAutoWidth(true);
		grid.addColumn(Survey::getTool).setHeader("Herramienta").setKey("tool").setAutoWidth(true);
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
		toolFilter.setPlaceholder("Filtrar por Herramienta");
		toolFilter.setItems(Tool.values());
		toolFilter.setItemLabelGenerator(Tool::name);
		toolFilter.setClearButtonVisible(true);


		// Añadir listeners para refrescar el grid
		nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		initDateFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		linkFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
		toolFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

		// Configurar el DataProvider del Grid
		grid.setItems(query -> {
			String nameVal = nameFilter.getValue();
			LocalDate initDateVal = initDateFilter.getValue();
			String linkVal = linkFilter.getValue();
			Tool toolVal = toolFilter.getValue();

			return surveyService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), nameVal, initDateVal, linkVal, toolVal)
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
		save.addClickShortcut(Key.ENTER);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Optional<Long> surveyId = event.getRouteParameters().get(SURVEY_ID).map(Long::parseLong);
		if (surveyId.isPresent()) {
			// Cargar la encuesta. La carga de participaciones dependerá de la configuración
            // EAGER/LAZY o se puede hacer explícitamente si es necesario más adelante.
			Optional<Survey> surveyFromBackend = surveyService.get(surveyId.get());
			if (surveyFromBackend.isPresent()) {
				populateForm(surveyFromBackend.get());
				// Las participaciones se cargarán al abrir el diálogo específico.
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
		tool = new ComboBox<>("Herramienta");
		tool.setItems(Tool.values());
		tool.setItemLabelGenerator(Tool::name); 
		formLayout.add(name, initDate, link, tool, viewParticipantsButton, sortearPanelistasButton, sendSurveysButton, sendReminderButton);


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
		headerRow.getCell(grid.getColumnByKey("tool")).setComponent(toolFilter);
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
        if (viewParticipantsButton != null) {
            viewParticipantsButton.setEnabled(value != null && value.getId() != null);
        }
        if (sortearPanelistasButton != null) {
            sortearPanelistasButton.setEnabled(value != null && value.getId() != null);
        }
        if (sendSurveysButton != null) {
            sendSurveysButton.setEnabled(value != null && value.getId() != null);
        }
        if (sendReminderButton != null) {
            sendReminderButton.setEnabled(value != null && value.getId() != null);
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
        if (viewParticipantsButton != null) {
            viewParticipantsButton.setEnabled(false);
        }
        if (sortearPanelistasButton != null) {
            sortearPanelistasButton.setEnabled(false);
        }
        if (sendSurveysButton != null) {
            sendSurveysButton.setEnabled(false);
        }
        if (sendReminderButton != null) {
            sendReminderButton.setEnabled(false);
        }
	}

    private void sendSurveysAction() {
        if (this.survey == null || this.survey.getId() == null) {
            Notification.show("Por favor, seleccione una encuesta.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Optional<Survey> surveyOpt = surveyService.getWithParticipations(this.survey.getId());
        if (surveyOpt.isEmpty()) {
            Notification.show("Encuesta no encontrada.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Survey currentSurveyWithParticipations = surveyOpt.get();
        Set<SurveyPanelistParticipation> participations = currentSurveyWithParticipations.getParticipations();

        if (participations == null || participations.isEmpty()) {
            Notification.show("No hay panelistas asignados a esta encuesta.", 3000, Notification.Position.MIDDLE);
            return;
        }

        int tasksCreated = 0;
        for (SurveyPanelistParticipation participation : participations) {
            // Panelist panelist = participation.getPanelist(); // No longer needed directly for MessageTask
            // if (panelist != null) { // Check if participation itself is valid if necessary
            if (participation != null && participation.getPanelist() != null) { // Ensure participation and its panelist are not null
                MessageTask mt = new MessageTask();
                mt.setJobType(JobType.ALCHEMER_INVITE);
                mt.setCreated(LocalDateTime.now());
                mt.setStatus(MessageTaskStatus.PENDING);
                mt.setSurveyPanelistParticipation(participation); // Set the participation
                messageTaskService.save(mt);
                tasksCreated++;
            }
        }
        Notification.show(tasksCreated + " tareas de mensaje creadas para los panelistas.", 6000, Notification.Position.MIDDLE);
    }

    private void sendReminderAction() {
        if (this.survey == null || this.survey.getId() == null) {
            Notification.show("Por favor, seleccione una encuesta.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Optional<Survey> surveyOpt = surveyService.getWithParticipations(this.survey.getId());
        if (surveyOpt.isEmpty()) {
            Notification.show("Encuesta no encontrada.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Survey currentSurveyWithParticipations = surveyOpt.get();
        Set<SurveyPanelistParticipation> participations = currentSurveyWithParticipations.getParticipations();

        if (participations == null || participations.isEmpty()) {
            Notification.show("No hay panelistas asignados a esta encuesta para enviar recordatorios.", 3000, Notification.Position.MIDDLE);
            return;
        }

        int tasksCreated = 0;
        for (SurveyPanelistParticipation participation : participations) {
            // Panelist panelist = participation.getPanelist(); // No longer needed directly for MessageTask
            // if (panelist != null) { // Check if participation itself is valid if necessary
            if (participation != null && participation.getPanelist() != null) { // Ensure participation and its panelist are not null
                MessageTask mt = new MessageTask();
                mt.setJobType(JobType.ALCHEMER_REMINDER);
                mt.setCreated(LocalDateTime.now());
                mt.setStatus(MessageTaskStatus.PENDING); // Assuming PENDING exists
                mt.setSurveyPanelistParticipation(participation); // Set the participation
                messageTaskService.save(mt);
                tasksCreated++;
            }
        }
        Notification.show(tasksCreated + " tareas de recordatorio creadas para los panelistas.", 6000, Notification.Position.MIDDLE);
    }

    private void openSortearPanelistasDialog() {
        if (this.survey == null || this.survey.getId() == null) {
            Notification.show("No hay encuesta seleccionada para sortear panelistas.", 3000, Notification.Position.MIDDLE);
            return;
        }

        Dialog sorteoDialog = new Dialog();
        sorteoDialog.setHeaderTitle("Sortear Panelistas para: " + this.survey.getName());
        sorteoDialog.setWidth("500px");

        VerticalLayout dialogLayout = new VerticalLayout();

        ComboBox<Panel> panelComboBox = new ComboBox<>("Seleccionar Panel");
        panelComboBox.setItems(panelService.findAll());
        panelComboBox.setItemLabelGenerator(Panel::getName);
        panelComboBox.setAllowCustomValue(true); // As requested
		panelComboBox.addCustomValueSetListener(e -> {
			String customValue = e.getDetail();
			// This is a basic handler. In a real app, you might try to find a panel by this custom name,
            // or provide an option to create a new one if it doesn't exist.
            // For now, if a custom value is entered that doesn't match an item,
            // it will likely just result in panelComboBox.getValue() being null unless it matches an existing name.
            // A more robust solution would involve checking if customValue matches any existing Panel names
            // or guiding the user. For now, we keep it simple as the main interaction is selection.
			Notification.show("Valor personalizado '" + customValue + "' ingresado. Si no coincide con un panel existente, no se seleccionará.", 3500, Position.MIDDLE);
		});


        IntegerField cantidadPanelistasField = new IntegerField("Cantidad de Panelistas a Sortear");
        cantidadPanelistasField.setPlaceholder("Ingrese un número entero");
        cantidadPanelistasField.setStepButtonsVisible(true);
        cantidadPanelistasField.setMin(1); // Panelists count must be at least 1

        dialogLayout.add(panelComboBox, cantidadPanelistasField);
        sorteoDialog.add(dialogLayout);

        Button cancelButton = new Button("Cancelar", e -> sorteoDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button doSortearButton = new Button("Sortear Panelistas", e -> {
            Panel selectedPanel = panelComboBox.getValue();
            Integer numToDraw = cantidadPanelistasField.getValue();

            if (selectedPanel == null) {
                Notification.show("Por favor, seleccione un panel.", 3000, Position.MIDDLE);
                return;
            }
            if (numToDraw == null || numToDraw <= 0) {
                Notification.show("Por favor, ingrese una cantidad válida de panelistas a sortear.", 3000, Position.MIDDLE);
                return;
            }

            // Fetch the panel with its panelists
            Optional<Panel> panelWithPanelistsOpt = panelService.getWithPanelists(selectedPanel.getId());
            if (panelWithPanelistsOpt.isEmpty()) {
                Notification.show("No se pudo cargar el panel seleccionado.", 3000, Position.MIDDLE);
                return;
            }
            Panel panelWithPanelists = panelWithPanelistsOpt.get();
            Set<Panelist> panelistsFromPanelSet = panelWithPanelists.getPanelists();

            if (panelistsFromPanelSet == null || panelistsFromPanelSet.isEmpty()) {
                Notification.show("El panel seleccionado no tiene panelistas.", 3000, Position.MIDDLE);
                return;
            }

            List<Panelist> panelistsList = new ArrayList<>(panelistsFromPanelSet);

            if (panelistsList.size() < numToDraw) {
                Notification.show("El panel seleccionado solo tiene " + panelistsList.size() + " panelistas. No se pueden sortear " + numToDraw + ".", 5000, Position.MIDDLE);
                return;
            }

            Collections.shuffle(panelistsList);
            List<Panelist> selectedPanelists = panelistsList.subList(0, numToDraw);

            int createdCount = 0;
            try {
                for (Panelist panelist : selectedPanelists) {
                    SurveyPanelistParticipation participation = new SurveyPanelistParticipation();
                    participation.setSurvey(this.survey);
                    participation.setPanelist(panelist);
                    participation.setDateIncluded(LocalDate.now());
                    participation.setCompleted(false);
                    participationService.save(participation);
                    createdCount++;
                }
                Notification.show(createdCount + " participaciones de panelistas creadas exitosamente.", 3000, Position.BOTTOM_START);
                sorteoDialog.close();
            } catch (Exception ex) {
                Notification.show("Error al crear participaciones: " + ex.getMessage(), 5000, Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        doSortearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        sorteoDialog.getFooter().add(cancelButton, doSortearButton);

        sorteoDialog.open();
    }

    private void openParticipantsDialog() {
        if (this.survey == null || this.survey.getId() == null) {
            Notification.show("No hay encuesta seleccionada.", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Cargar las participaciones para la encuesta actual
        // Esto asume que tienes un método en participationService para obtener participaciones por Survey ID
        // o que la relación Survey -> SurveyPanelistParticipation está configurada como EAGER
        // o se carga explícitamente al obtener la encuesta.
        // Para ser explícitos, podríamos hacer:
        // List<SurveyPanelistParticipation> participations = participationService.findBySurveyId(this.survey.getId());
        // O, si la entidad Survey ya tiene las participaciones (por carga EAGER o JOIN FETCH previo):
        Survey currentSurvey = surveyService.getWithParticipations(this.survey.getId()).orElse(null); // Changed to getWithParticipations
        if (currentSurvey == null || currentSurvey.getParticipations() == null || currentSurvey.getParticipations().isEmpty()) {
            Notification.show("No hay participaciones para esta encuesta.", 3000, Notification.Position.MIDDLE);
            return;
        }


        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Participaciones de la Encuesta: " + currentSurvey.getName());
        dialog.setWidth("80%");
        dialog.setHeight("70%");

        Grid<SurveyPanelistParticipation> participationsGrid = new Grid<>(SurveyPanelistParticipation.class, false);
        participationsGrid.addColumn(participation -> participation.getPanelist().getFirstName()).setHeader("Nombre Panelista").setSortable(true).setKey("panelistFirstName");
        participationsGrid.addColumn(participation -> participation.getPanelist().getLastName()).setHeader("Apellido Panelista").setSortable(true).setKey("panelistLastName");
        participationsGrid.addColumn(participation -> participation.getPanelist().getEmail()).setHeader("Email Panelista").setSortable(true).setKey("panelistEmail"); // Added Email column
        Grid.Column<SurveyPanelistParticipation> dateIncludedColumn = participationsGrid.addColumn(SurveyPanelistParticipation::getDateIncluded).setHeader("Fecha Inclusión").setSortable(true).setKey("dateIncluded");
        Grid.Column<SurveyPanelistParticipation> dateSentColumn = participationsGrid.addColumn(SurveyPanelistParticipation::getDateSent).setHeader("Fecha Ult. Envío").setSortable(true).setKey("dateSent");
        Grid.Column<SurveyPanelistParticipation> completedColumn = participationsGrid.addColumn(SurveyPanelistParticipation::isCompleted).setHeader("Completada").setSortable(true).setKey("completed");


        // Add filters
        HeaderRow filterRow = participationsGrid.appendHeaderRow();

        TextField panelistFirstNameFilter = new TextField();
        panelistFirstNameFilter.setPlaceholder("Filtrar...");
        panelistFirstNameFilter.setClearButtonVisible(true);
        filterRow.getCell(participationsGrid.getColumnByKey("panelistFirstName")).setComponent(panelistFirstNameFilter);

        TextField panelistLastNameFilter = new TextField();
        panelistLastNameFilter.setPlaceholder("Filtrar...");
        panelistLastNameFilter.setClearButtonVisible(true);
        filterRow.getCell(participationsGrid.getColumnByKey("panelistLastName")).setComponent(panelistLastNameFilter);

        TextField panelistEmailFilter = new TextField(); // Added Email filter
        panelistEmailFilter.setPlaceholder("Filtrar...");
        panelistEmailFilter.setClearButtonVisible(true);
        filterRow.getCell(participationsGrid.getColumnByKey("panelistEmail")).setComponent(panelistEmailFilter);

        DatePicker dateIncludedFilter = new DatePicker();
        dateIncludedFilter.setPlaceholder("Filtrar Fecha");
        dateIncludedFilter.setClearButtonVisible(true);
        filterRow.getCell(dateIncludedColumn).setComponent(dateIncludedFilter);

        DatePicker dateSentFilter = new DatePicker();
        dateSentFilter.setPlaceholder("Filtrar Fecha");
        dateSentFilter.setClearButtonVisible(true);
        filterRow.getCell(dateSentColumn).setComponent(dateSentFilter);

        ComboBox<String> completedFilter = new ComboBox<>();
        completedFilter.setPlaceholder("Todos");
        completedFilter.setItems("Sí", "No", "Todos");
        completedFilter.setClearButtonVisible(true); // Though "Todos" acts as a clearer
        filterRow.getCell(completedColumn).setComponent(completedFilter);


        participationsGrid.setItems(query -> currentSurvey.getParticipations().stream()
                .filter(participation -> {
                    // Panelist First Name Filter
                    boolean firstNameMatch = panelistFirstNameFilter.getValue() == null ||
                                             panelistFirstNameFilter.getValue().isBlank() ||
                                             participation.getPanelist().getFirstName().toLowerCase().contains(panelistFirstNameFilter.getValue().toLowerCase());
                    // Panelist Last Name Filter
                    boolean lastNameMatch = panelistLastNameFilter.getValue() == null ||
                                            panelistLastNameFilter.getValue().isBlank() ||
                                            participation.getPanelist().getLastName().toLowerCase().contains(panelistLastNameFilter.getValue().toLowerCase());
                    // Panelist Email Filter
                    boolean emailMatch = panelistEmailFilter.getValue() == null ||
                                         panelistEmailFilter.getValue().isBlank() ||
                                         participation.getPanelist().getEmail().toLowerCase().contains(panelistEmailFilter.getValue().toLowerCase());
                    // Date Included Filter
                    boolean dateIncludedMatch = dateIncludedFilter.getValue() == null ||
                                                dateIncludedFilter.getValue().equals(participation.getDateIncluded());
                    // Date Sent Filter
                    boolean dateSentMatch = dateSentFilter.getValue() == null ||
                                            (participation.getDateSent() != null && dateSentFilter.getValue().equals(participation.getDateSent()));
                    // Completed Filter
                    boolean completedMatch = true;
                    String completedValue = completedFilter.getValue();
                    if (completedValue != null && !completedValue.equals("Todos")) {
                        boolean isCompletedTarget = completedValue.equals("Sí");
                        completedMatch = participation.isCompleted() == isCompletedTarget;
                    }
                    return firstNameMatch && lastNameMatch && emailMatch && dateIncludedMatch && dateSentMatch && completedMatch; // Added emailMatch
                })
                .skip(query.getOffset())
                .limit(query.getLimit())
        );

        panelistFirstNameFilter.addValueChangeListener(e -> participationsGrid.getDataProvider().refreshAll());
        panelistLastNameFilter.addValueChangeListener(e -> participationsGrid.getDataProvider().refreshAll());
        panelistEmailFilter.addValueChangeListener(e -> participationsGrid.getDataProvider().refreshAll()); // Added listener for email filter
        dateIncludedFilter.addValueChangeListener(e -> participationsGrid.getDataProvider().refreshAll());
        dateSentFilter.addValueChangeListener(e -> participationsGrid.getDataProvider().refreshAll());
        completedFilter.addValueChangeListener(e -> participationsGrid.getDataProvider().refreshAll());

        dialog.add(participationsGrid);
        Button closeButton = new Button("Cerrar", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
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
