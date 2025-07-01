package uy.com.equipos.panelmanagement.views;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import uy.com.equipos.panelmanagement.data.MessageTask;
import uy.com.equipos.panelmanagement.services.MessageTaskService;
import uy.com.equipos.panelmanagement.data.JobType; // Required for JobType
import uy.com.equipos.panelmanagement.data.MessageTaskStatus; // Required for MessageTaskStatus

import com.vaadin.flow.component.textfield.TextField; // Required for TextField
import com.vaadin.flow.data.value.ValueChangeMode; // Required for ValueChangeMode
import com.vaadin.flow.component.grid.HeaderRow; // Required for HeaderRow

import java.time.LocalDateTime; // Required for LocalDateTime
import java.time.format.DateTimeFormatter; // Required for DateTimeFormatter
import java.util.List;
import java.util.Objects; // Required for Objects.toString

@PageTitle("Tareas")
@Route(value = "messagetasks", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class MessageTaskView extends Div implements HasComponents, HasStyle {

    private Grid<MessageTask> grid = new Grid<>(MessageTask.class, false);
    private GridListDataView<MessageTask> gridListDataView;

    private final MessageTaskService messageTaskService;

    @Autowired
    public MessageTaskView(MessageTaskService messageTaskService) {
        this.messageTaskService = messageTaskService;
        addClassNames("message-task-view");
        setSizeFull();
        configureGrid();
        loadGridData();
        add(grid);
    }

    private void configureGrid() {
        HeaderRow filterRow = grid.appendHeaderRow();

        Grid.Column<MessageTask> idColumn = grid.addColumn(MessageTask::getId).setHeader("Id").setAutoWidth(true).setSortable(true);
        TextField idFilter = createTextFieldFilter(gridListDataView, MessageTask::getId);
        filterRow.getCell(idColumn).setComponent(idFilter);

        Grid.Column<MessageTask> jobTypeColumn = grid.addColumn(MessageTask::getJobType).setHeader("Job Type").setAutoWidth(true).setSortable(true);
        TextField jobTypeFilter = createTextFieldFilter(gridListDataView, MessageTask::getJobType);
        filterRow.getCell(jobTypeColumn).setComponent(jobTypeFilter);

        Grid.Column<MessageTask> createdColumn = grid.addColumn(MessageTask::getCreated).setHeader("Created").setAutoWidth(true).setSortable(true);
        TextField createdFilter = createTextFieldFilter(gridListDataView, mt -> formatLocalDateTime(mt.getCreated()));
        filterRow.getCell(createdColumn).setComponent(createdFilter);

        Grid.Column<MessageTask> statusColumn = grid.addColumn(MessageTask::getStatus).setHeader("Status").setAutoWidth(true).setSortable(true);
        TextField statusFilter = createTextFieldFilter(gridListDataView, MessageTask::getStatus);
        filterRow.getCell(statusColumn).setComponent(statusFilter);

        Grid.Column<MessageTask> participationIdColumn = grid.addColumn(mt -> mt.getSurveyPanelistParticipation() != null ? mt.getSurveyPanelistParticipation().getId() : null)
            .setHeader("Survey Panelist Participation Id").setAutoWidth(true).setSortable(true);
        TextField participationIdFilter = createTextFieldFilter(gridListDataView, mt -> mt.getSurveyPanelistParticipation() != null ? mt.getSurveyPanelistParticipation().getId() : "");
        filterRow.getCell(participationIdColumn).setComponent(participationIdFilter);

        Grid.Column<MessageTask> surveyColumn = grid.addColumn(mt -> mt.getSurvey() != null ? mt.getSurvey().getName() : null)
            .setHeader("Survey").setAutoWidth(true).setSortable(true);
        TextField surveyFilter = createTextFieldFilter(gridListDataView, mt -> mt.getSurvey() != null ? mt.getSurvey().getName() : "");
        filterRow.getCell(surveyColumn).setComponent(surveyFilter);

        grid.setSizeFull();
    }

    private String formatLocalDateTime(LocalDateTime ldt) {
        if (ldt == null) return "";
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private TextField createTextFieldFilter(GridListDataView<MessageTask> dataView, ValueProvider<MessageTask, ?> valueProvider) {
        TextField filterField = new TextField();
        filterField.setWidthFull();
        filterField.setPlaceholder("Filter");
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.addValueChangeListener(event -> {
            dataView.removeFilters(); // Clear previous filters
            if (!event.getValue().isEmpty()) {
                dataView.addFilter(task ->
                    Objects.toString(valueProvider.apply(task), "").toLowerCase().contains(event.getValue().toLowerCase())
                );
            }
        });
        return filterField;
    }

    // Functional interface for accessing property values, needed for createTextFieldFilter
    @FunctionalInterface
    interface ValueProvider<T, R> {
        R apply(T t);
    }

    private void loadGridData() {
        List<MessageTask> messageTasks = messageTaskService.findAll();
        gridListDataView = grid.setItems(messageTasks);
        // Re-apply filters when data is loaded, if any filters were set previously
        // This is more relevant if filters are persisted or set before data loading.
        // For now, filters are applied on value change.
    }
}
