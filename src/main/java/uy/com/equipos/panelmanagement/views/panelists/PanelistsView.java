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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<Panelist> binder;

    private Panelist panelist;

    private final PanelistService panelistService;

    public PanelistsView(PanelistService panelistService) {
        this.panelistService = panelistService;
        addClassNames("panelists-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configurar placeholders para filtros
        firstNameFilter.setPlaceholder("Filtrar por nombre");
        lastNameFilter.setPlaceholder("Filtrar por apellido");
        emailFilter.setPlaceholder("Filtrar por email");
        phoneFilter.setPlaceholder("Filtrar por teléfono");
        dateOfBirthFilter.setPlaceholder("Filtrar por fecha nac.");
        occupationFilter.setPlaceholder("Filtrar por ocupación");
        lastContactedFilter.setPlaceholder("Filtrar por últ. contacto");
        lastInterviewedFilter.setPlaceholder("Filtrar por últ. entrevista");

        // Añadir listeners para refrescar el grid
        firstNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        lastNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        emailFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        phoneFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        dateOfBirthFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        occupationFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        lastContactedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        lastInterviewedFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        // Configure Grid
        grid.addColumn("firstName").setKey("firstName").setAutoWidth(true);
        grid.addColumn("lastName").setKey("lastName").setAutoWidth(true);
        grid.addColumn("email").setKey("email").setAutoWidth(true);
        grid.addColumn("phone").setKey("phone").setAutoWidth(true);
        grid.addColumn("dateOfBirth").setKey("dateOfBirth").setAutoWidth(true);
        grid.addColumn("occupation").setKey("occupation").setAutoWidth(true);
        grid.addColumn("lastContacted").setKey("lastContacted").setAutoWidth(true);
        grid.addColumn("lastInterviewed").setKey("lastInterviewed").setAutoWidth(true);

        grid.setItems(query -> {
            String firstName = firstNameFilter.getValue();
            String lastName = lastNameFilter.getValue();
            String email = emailFilter.getValue();
            String phone = phoneFilter.getValue();
            LocalDate dateOfBirth = dateOfBirthFilter.getValue();
            String occupation = occupationFilter.getValue();
            LocalDate lastContacted = lastContactedFilter.getValue();
            LocalDate lastInterviewed = lastInterviewedFilter.getValue();

            return panelistService.list(
                VaadinSpringDataHelpers.toSpringPageRequest(query),
                firstName,
                lastName,
                email,
                phone,
                dateOfBirth,
                occupation,
                lastContacted,
                lastInterviewed
            ).stream();
        });
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(PANELIST_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
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
                Notification.show("Data updated");
                UI.getCurrent().navigate(PanelistsView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
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
            } else {
                Notification.show(String.format("The requested panelist was not found, ID = %s", panelistId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(PanelistsView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        firstName = new TextField("First Name");
        lastName = new TextField("Last Name");
        email = new TextField("Email");
        phone = new TextField("Phone");
        dateOfBirth = new DatePicker("Date Of Birth");
        occupation = new TextField("Occupation");
        lastContacted = new DatePicker("Last Contacted");
        lastInterviewed = new DatePicker("Last Interviewed");
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
    }

    private void populateForm(Panelist value) {
        this.panelist = value;
        binder.readBean(this.panelist);

    }
}
