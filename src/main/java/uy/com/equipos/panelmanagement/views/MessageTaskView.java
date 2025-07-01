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

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.datepicker.DatePicker; // Added for DatePicker
import com.vaadin.flow.component.combobox.ComboBox; // Added for ComboBox

import java.time.LocalDate; // Added for LocalDate comparison
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap; // Added for filter map
import java.util.List;
import java.util.Map; // Added for filter map
import java.util.Objects;
import java.util.function.Predicate; // Added for Predicate

@PageTitle("Tareas")
@Route(value = "messagetasks", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class MessageTaskView extends Div implements HasComponents, HasStyle {

    private Grid<MessageTask> grid = new Grid<>(MessageTask.class, false);
    private GridListDataView<MessageTask> gridListDataView;
    private final Map<String, Predicate<MessageTask>> activeFilters = new HashMap<>();

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
        TextField idFilter = createTextFieldFilter("id", MessageTask::getId);
        filterRow.getCell(idColumn).setComponent(idFilter);

        Grid.Column<MessageTask> jobTypeColumn = grid.addColumn(MessageTask::getJobType).setHeader("Job Type").setAutoWidth(true).setSortable(true);
        ComboBox<JobType> jobTypeComboBox = createComboBoxFilter("jobType", JobType.class, JobType.values(), MessageTask::getJobType);
        filterRow.getCell(jobTypeColumn).setComponent(jobTypeComboBox);

        Grid.Column<MessageTask> createdColumn = grid.addColumn(MessageTask::getCreated).setHeader("Created").setAutoWidth(true).setSortable(true);
        DatePicker createdDatePicker = createDatePickerFilter("createdDate");
        filterRow.getCell(createdColumn).setComponent(createdDatePicker);

        Grid.Column<MessageTask> statusColumn = grid.addColumn(MessageTask::getStatus).setHeader("Status").setAutoWidth(true).setSortable(true);
        ComboBox<MessageTaskStatus> statusComboBox = createComboBoxFilter("status", MessageTaskStatus.class, MessageTaskStatus.values(), MessageTask::getStatus);
        filterRow.getCell(statusColumn).setComponent(statusComboBox);

        Grid.Column<MessageTask> participationIdColumn = grid.addColumn(mt -> mt.getSurveyPanelistParticipation() != null ? mt.getSurveyPanelistParticipation().getId() : null)
            .setHeader("Survey Panelist Participation Id").setAutoWidth(true).setSortable(true);
        TextField participationIdFilter = createTextFieldFilter("participationId", mt -> mt.getSurveyPanelistParticipation() != null ? mt.getSurveyPanelistParticipation().getId() : "");
        filterRow.getCell(participationIdColumn).setComponent(participationIdFilter);

        Grid.Column<MessageTask> surveyColumn = grid.addColumn(mt -> mt.getSurvey() != null ? mt.getSurvey().getName() : null)
            .setHeader("Survey").setAutoWidth(true).setSortable(true);
        TextField surveyFilter = createTextFieldFilter("surveyName", mt -> mt.getSurvey() != null ? mt.getSurvey().getName() : "");
        filterRow.getCell(surveyColumn).setComponent(surveyFilter);

        grid.setSizeFull();
    }

    private String formatLocalDateTime(LocalDateTime ldt) {
        if (ldt == null) return "";
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private DatePicker createDatePickerFilter(String filterKey) {
        DatePicker datePicker = new DatePicker();
        datePicker.setWidthFull();
        datePicker.setPlaceholder("Filter by date");
        datePicker.setClearButtonVisible(true);
        datePicker.addValueChangeListener(event -> {
            LocalDate selectedDate = event.getValue();
            if (selectedDate == null) {
                activeFilters.remove(filterKey);
            } else {
                activeFilters.put(filterKey, (Predicate<MessageTask> & java.io.Serializable) task -> {
                    if (task.getCreated() == null) return false;
                    return task.getCreated().toLocalDate().equals(selectedDate);
                });
            }
            applyFilters();
        });
        return datePicker;
    }

    private <E extends Enum<E>> ComboBox<E> createComboBoxFilter(String filterKey, Class<E> enumClass, E[] enumValues, ValueProvider<MessageTask, E> valueProvider) {
        ComboBox<E> comboBox = new ComboBox<>();
        comboBox.setWidthFull();
        comboBox.setPlaceholder("Filter by " + enumClass.getSimpleName());
        comboBox.setItems(enumValues);
        comboBox.setClearButtonVisible(true);
        comboBox.addValueChangeListener(event -> {
            E selectedValue = event.getValue();
            if (selectedValue == null) {
                activeFilters.remove(filterKey);
            } else {
                activeFilters.put(filterKey, (Predicate<MessageTask> & java.io.Serializable) task -> {
                    E taskValue = valueProvider.apply(task);
                    return taskValue != null && taskValue.equals(selectedValue);
                });
            }
            applyFilters();
        });
        return comboBox;
    }

    private TextField createTextFieldFilter(String filterKey, ValueProvider<MessageTask, ?> valueProvider) {
        TextField filterField = new TextField();
        filterField.setWidthFull();
        filterField.setPlaceholder("Filter");
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.addValueChangeListener(event -> {
            String filterValue = event.getValue().toLowerCase();
            if (filterValue.isEmpty()) {
                activeFilters.remove(filterKey);
            } else {
                activeFilters.put(filterKey, (Predicate<MessageTask> & java.io.Serializable) task ->
                    Objects.toString(valueProvider.apply(task), "").toLowerCase().contains(filterValue)
                );
            }
            applyFilters();
        });
        return filterField;
    }

    private void applyFilters() {
        if (gridListDataView == null) {
            return;
        }
        // Ensure the combined predicate is serializable
        Predicate<MessageTask> combinedFilter = activeFilters.values().stream()
            .reduce((java.io.Serializable & Predicate<MessageTask>) Predicate::and)
            .orElse((java.io.Serializable & Predicate<MessageTask>) task -> true); // If no filters, show all (must also be serializable)

        // The setFilter method expects a SerializablePredicate.
        // Our combinedFilter should now conform if all individual predicates were serializable.
        gridListDataView.setFilter((com.vaadin.flow.function.SerializablePredicate<MessageTask>) combinedFilter);
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
