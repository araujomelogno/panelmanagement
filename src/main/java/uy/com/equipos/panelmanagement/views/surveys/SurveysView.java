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

    // Campos de filtro
    private TextField nameFilter = new TextField();
    private DatePicker initDateFilter = new DatePicker();
    private TextField linkFilter = new TextField();

    private TextField name;
    private DatePicker initDate;
    private TextField link;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<Survey> binder;

    private Survey survey;

    private final SurveyService surveyService;

    public SurveysView(SurveyService surveyService) {
        this.surveyService = surveyService;
        addClassNames("surveys-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configurar placeholders para filtros
        nameFilter.setPlaceholder("Filtrar por nombre");
        initDateFilter.setPlaceholder("Filtrar por fecha");
        linkFilter.setPlaceholder("Filtrar por link");

        // Añadir listeners para refrescar el grid
        nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        initDateFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        linkFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        // Configure Grid
        grid.addColumn("name").setKey("name").setAutoWidth(true);
        grid.addColumn("initDate").setKey("initDate").setAutoWidth(true);
        grid.addColumn("link").setKey("link").setAutoWidth(true);
        grid.setItems(query -> {
            String name = nameFilter.getValue();
            LocalDate initDate = initDateFilter.getValue();
            String link = linkFilter.getValue();

            return surveyService.list(
                VaadinSpringDataHelpers.toSpringPageRequest(query),
                name,
                initDate,
                link
            ).stream();
        });
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(SURVEY_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
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
                Notification.show("Data updated");
                UI.getCurrent().navigate(SurveysView.class);
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
        Optional<Long> surveyId = event.getRouteParameters().get(SURVEY_ID).map(Long::parseLong);
        if (surveyId.isPresent()) {
            Optional<Survey> surveyFromBackend = surveyService.get(surveyId.get());
            if (surveyFromBackend.isPresent()) {
                populateForm(surveyFromBackend.get());
            } else {
                Notification.show(String.format("The requested survey was not found, ID = %s", surveyId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(SurveysView.class);
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
        name = new TextField("Name");
        initDate = new DatePicker("Init Date");
        link = new TextField("Link");
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
        buttonLayout.add(save, cancel);
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

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Survey value) {
        this.survey = value;
        binder.readBean(this.survey);

    }
}
