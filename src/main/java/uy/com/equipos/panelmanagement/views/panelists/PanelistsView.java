package uy.com.equipos.panelmanagement.views.panelists;

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
import uy.com.equipos.panelmanagement.data.Panelist;
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
    private DatePicker dateOfBirthFilter = new DatePicker();
    private TextField occupationFilter = new TextField();
    private DatePicker lastContactedFilter = new DatePicker();
    private DatePicker lastInterviewedFilter = new DatePicker();

    private TextField firstName;
    private TextField lastName;
    private TextField email;
    private TextField phone;
    private DatePicker dateOfBirth;
    private TextField occupation;
    private DatePicker lastContacted;
    private DatePicker lastInterviewed;

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");

    private Button nuevoPanelistaButton;


    private final BeanValidationBinder<Panelist> binder;

    private Panelist panelist;

    private final PanelistService panelistService;

    public PanelistsView(PanelistService panelistService) {
        this.panelistService = panelistService;
        addClassNames("panelists-view");


        grid.addColumn(Panelist::getFirstName).setHeader("Nombre").setKey("firstName").setAutoWidth(true);
        grid.addColumn(Panelist::getLastName).setHeader("Apellido").setKey("lastName").setAutoWidth(true);
        grid.addColumn(Panelist::getEmail).setHeader("Correo Electrónico").setKey("email").setAutoWidth(true);
        grid.addColumn(Panelist::getPhone).setHeader("Teléfono").setKey("phone").setAutoWidth(true);
        grid.addColumn(Panelist::getDateOfBirth).setHeader("Fecha de Nacimiento").setKey("dateOfBirth").setAutoWidth(true);
        grid.addColumn(Panelist::getOccupation).setHeader("Ocupación").setKey("occupation").setAutoWidth(true);
        grid.addColumn(Panelist::getLastContacted).setHeader("Último Contacto").setKey("lastContacted").setAutoWidth(true);
        grid.addColumn(Panelist::getLastInterviewed).setHeader("Última Entrevista").setKey("lastInterviewed").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // Create UI - SplitLayout
        SplitLayout splitLayout = new SplitLayout();
        // createGridLayout ahora puede acceder a las keys de las columnas de forma segura
        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);
        // editorLayoutDiv.setVisible(false); // Se maneja después de add(mainLayout)

        // Crear barra de título
        H2 pageTitleText = new H2("Panelistas");
        nuevoPanelistaButton = new Button("Nuevo Panelista");
        HorizontalLayout titleBar = new HorizontalLayout(pageTitleText, nuevoPanelistaButton);
        titleBar.setWidthFull();
        titleBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        titleBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout mainLayout = new VerticalLayout(titleBar, splitLayout);
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
 
        editorLayoutDiv.setVisible(false); // Ocultar el editor inicialmente
        add(splitLayout);



        firstNameFilter.setPlaceholder("Filtrar por Nombre");
        lastNameFilter.setPlaceholder("Filtrar por Apellido");
        emailFilter.setPlaceholder("Filtrar por Correo Electrónico");
        phoneFilter.setPlaceholder("Filtrar por Teléfono");
        dateOfBirthFilter.setPlaceholder("Filtrar por Fecha de Nacimiento");
        occupationFilter.setPlaceholder("Filtrar por Ocupación");
        lastContactedFilter.setPlaceholder("Filtrar por Último Contacto");
        lastInterviewedFilter.setPlaceholder("Filtrar por Última Entrevista");

        // Añadir listeners para refrescar el grid
        firstNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        lastNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        emailFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        phoneFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        dateOfBirthFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        occupationFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        lastContactedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        lastInterviewedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        // Configurar el DataProvider del Grid
        grid.setItems(query -> {
            String firstNameVal = firstNameFilter.getValue();
            String lastNameVal = lastNameFilter.getValue();
            String emailVal = emailFilter.getValue();
            String phoneVal = phoneFilter.getValue();
            LocalDate dateOfBirthVal = dateOfBirthFilter.getValue();
            String occupationVal = occupationFilter.getValue();
            LocalDate lastContactedVal = lastContactedFilter.getValue();
            LocalDate lastInterviewedVal = lastInterviewedFilter.getValue();

            return panelistService.list(
                VaadinSpringDataHelpers.toSpringPageRequest(query),
                firstNameVal,
                lastNameVal,
                emailVal,
                phoneVal,
                dateOfBirthVal,
                occupationVal,
                lastContactedVal,
                lastInterviewedVal
            ).stream();
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
                Notification.show("Fallo al actualizar los datos. Verifique nuevamente que todos los valores sean válidos");
            }
        });
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
        dateOfBirth = new DatePicker("Fecha de Nacimiento");
        occupation = new TextField("Ocupación");
        lastContacted = new DatePicker("Último Contacto");
        lastInterviewed = new DatePicker("Última Entrevista");
        formLayout.add(firstName, lastName, email, phone, dateOfBirth, occupation, lastContacted, lastInterviewed);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
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

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(grid.getColumnByKey("firstName")).setComponent(firstNameFilter);
        headerRow.getCell(grid.getColumnByKey("lastName")).setComponent(lastNameFilter);
        headerRow.getCell(grid.getColumnByKey("email")).setComponent(emailFilter);
        headerRow.getCell(grid.getColumnByKey("phone")).setComponent(phoneFilter);
        headerRow.getCell(grid.getColumnByKey("dateOfBirth")).setComponent(dateOfBirthFilter);
        headerRow.getCell(grid.getColumnByKey("occupation")).setComponent(occupationFilter);
        headerRow.getCell(grid.getColumnByKey("lastContacted")).setComponent(lastContactedFilter);
        headerRow.getCell(grid.getColumnByKey("lastInterviewed")).setComponent(lastInterviewedFilter);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
        if (editorLayoutDiv != null) { // Buena práctica verificar nulidad
            editorLayoutDiv.setVisible(false);
        }
    }

    private void populateForm(Panelist value) {
        this.panelist = value;
        binder.readBean(this.panelist);

    }
}
