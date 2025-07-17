package uy.com.equipos.panelmanagement.views;

import java.time.LocalDate; // Added for LocalDate comparison
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap; // Added for filter map
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.combobox.ComboBox; // Added for ComboBox
import com.vaadin.flow.component.datepicker.DatePicker; // Added for DatePicker
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate; // Added for SerializablePredicate
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import uy.com.equipos.panelmanagement.data.JobType; // Required for JobType
import uy.com.equipos.panelmanagement.data.Task;
import uy.com.equipos.panelmanagement.data.TaskStatus; // Required for MessageTaskStatus
import uy.com.equipos.panelmanagement.services.TaskService;

@PageTitle("Tareas")
@Route(value = "tasks", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class TaskView extends Div implements HasComponents, HasStyle {

    private Grid<Task> grid = new Grid<>(Task.class, false);
    private GridListDataView<Task> gridListDataView;
    private final Map<String, SerializablePredicate<Task>> activeFilters = new HashMap<>();

    private final TaskService messageTaskService;

    @Autowired
    public TaskView(TaskService messageTaskService) {
        this.messageTaskService = messageTaskService;
        addClassNames("message-task-view");
        setSizeFull();
        configureGrid();
        loadGridData();
        add(grid);
    }

    private void configureGrid() {
        // Define columns with keys first
        grid.addColumn(Task::getId).setHeader("Id").setAutoWidth(true).setSortable(true).setKey("id");
        grid.addColumn(Task::getJobType).setHeader("Tarea").setAutoWidth(true).setSortable(true).setKey("jobType");
        grid.addColumn(Task::getCreated).setHeader("Creada").setAutoWidth(true).setSortable(true).setKey("created");
        grid.addColumn(Task::getStatus).setHeader("Estado").setAutoWidth(true).setSortable(true).setKey("status");
        grid.addColumn(mt -> mt.getPanelist() != null ? mt.getPanelist().getFullName() : null)
            .setHeader("Panelista").setAutoWidth(true).setSortable(true).setKey("panelist");
        grid.addColumn(mt -> mt.getSurvey() != null ? mt.getSurvey().getName() : null)
            .setHeader("Encuesta").setAutoWidth(true).setSortable(true).setKey("surveyName");

        // Append the filter row
        HeaderRow filterRow = grid.appendHeaderRow();

        // Create and assign filters to the header row using column keys
        TextField idFilter = createTextFieldFilter("id_filter_key", Task::getId); // Changed filterKey to avoid conflict with column key if map keys are column keys
        filterRow.getCell(grid.getColumnByKey("id")).setComponent(idFilter);

        ComboBox<JobType> jobTypeComboBox = createComboBoxFilter("jobType_filter_key", JobType.class, JobType.values(), Task::getJobType);
        filterRow.getCell(grid.getColumnByKey("jobType")).setComponent(jobTypeComboBox);

        DatePicker createdDatePicker = createDatePickerFilter("createdDate_filter_key");
        filterRow.getCell(grid.getColumnByKey("created")).setComponent(createdDatePicker);

        ComboBox<TaskStatus> statusComboBox = createComboBoxFilter("status_filter_key", TaskStatus.class, TaskStatus.values(), Task::getStatus);
        filterRow.getCell(grid.getColumnByKey("status")).setComponent(statusComboBox);

        TextField panelistFilter = createTextFieldFilter("panelist_filter_key", mt -> mt.getPanelist() != null ? mt.getPanelist().getFullName() : "");
        filterRow.getCell(grid.getColumnByKey("panelist")).setComponent(panelistFilter);

        TextField surveyFilter = createTextFieldFilter("surveyName_filter_key", mt -> mt.getSurvey() != null ? mt.getSurvey().getName() : "");
        filterRow.getCell(grid.getColumnByKey("surveyName")).setComponent(surveyFilter);

        grid.setSizeFull();
    }

    private String formatLocalDateTime(LocalDateTime ldt) {
        if (ldt == null) return "";
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private DatePicker createDatePickerFilter(String filterKey) {
        DatePicker datePicker = new DatePicker();
        datePicker.setWidthFull();
        // Apply Spanish locale for dd/MM/yyyy format
        // Create a custom I18n object for DatePicker
        DatePicker.DatePickerI18n dpI18n = new DatePicker.DatePickerI18n();
        dpI18n.setDateFormat("dd/MM/yyyy");
        // You can customize other i18n properties here if needed
        // For example, month names, weekdays, etc.
        // dpI18n.setMonthNames(List.of("Enero", "Febrero", ..., "Diciembre"));
        // dpI18n.setWeekdays(List.of("Domingo", "Lunes", ..., "Sábado"));
        // dpI18n.setToday("Hoy");
        // dpI18n.setCancel("Cancelar");
        // dpI18n.setFirstDayOfWeek(1); // Monday

        datePicker.setI18n(dpI18n);
        datePicker.setLocale(new Locale("es", "UY")); // Spanish, Uruguay for dd/MM/yyyy
        datePicker.setPlaceholder("dd/MM/yyyy");
        datePicker.setClearButtonVisible(true);
        datePicker.addValueChangeListener(event -> {
            LocalDate selectedDate = event.getValue();
            if (selectedDate == null) {
                activeFilters.remove(filterKey);
            } else {
                activeFilters.put(filterKey, (SerializablePredicate<Task>) task -> {
                    if (task.getCreated() == null) return false;
                    return task.getCreated().toLocalDate().equals(selectedDate);
                });
            }
            applyFilters();
        });
        return datePicker;
    }

    private <E extends Enum<E>> ComboBox<E> createComboBoxFilter(String filterKey, Class<E> enumClass, E[] enumValues, ValueProvider<Task, E> valueProvider) {
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
                activeFilters.put(filterKey, (SerializablePredicate<Task>) task -> {
                    E taskValue = valueProvider.apply(task);
                    return taskValue != null && taskValue.equals(selectedValue);
                });
            }
            applyFilters();
        });
        return comboBox;
    }

    private TextField createTextFieldFilter(String filterKey, ValueProvider<Task, ?> valueProvider) {
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
                activeFilters.put(filterKey, (SerializablePredicate<Task>) task ->
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

        SerializablePredicate<Task> combinedFilter = activeFilters.values().stream()
            .reduce(SerializablePredicate::and)
            .orElse(task -> true); // This lambda is intrinsically serializable if it captures no non-serializable state

        gridListDataView.setFilter(combinedFilter);
    }

    // Functional interface for accessing property values, needed for createTextFieldFilter
    @FunctionalInterface
    interface ValueProvider<T, R> {
        R apply(T t);
    }

    private void loadGridData() {
        List<Task> messageTasks = messageTaskService.findAll();
        gridListDataView = grid.setItems(messageTasks);
        // Re-apply filters when data is loaded, if any filters were set previously
        // This is more relevant if filters are persisted or set before data loading.
        // For now, filters are applied on value change.
    }
}
