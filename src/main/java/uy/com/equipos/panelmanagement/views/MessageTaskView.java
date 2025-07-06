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
import uy.com.equipos.panelmanagement.data.MessageTask;
import uy.com.equipos.panelmanagement.data.MessageTaskStatus; // Required for MessageTaskStatus
import uy.com.equipos.panelmanagement.services.MessageTaskService;

@PageTitle("Tareas")
@Route(value = "messagetasks", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class MessageTaskView extends Div implements HasComponents, HasStyle {

    private Grid<MessageTask> grid = new Grid<>(MessageTask.class, false);
    private GridListDataView<MessageTask> gridListDataView;
    private final Map<String, SerializablePredicate<MessageTask>> activeFilters = new HashMap<>();

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
        // Define columns with keys first
        grid.addColumn(MessageTask::getId).setHeader("Id").setAutoWidth(true).setSortable(true).setKey("id");
        grid.addColumn(MessageTask::getJobType).setHeader("Tarea").setAutoWidth(true).setSortable(true).setKey("jobType");
        grid.addColumn(MessageTask::getCreated).setHeader("Creada").setAutoWidth(true).setSortable(true).setKey("created");
        grid.addColumn(MessageTask::getStatus).setHeader("Estado").setAutoWidth(true).setSortable(true).setKey("status");
        grid.addColumn(mt -> mt.getSurveyPanelistParticipation() != null ? mt.getSurveyPanelistParticipation().getPanelist().getFullName(): null)
            .setHeader("Panelista").setAutoWidth(true).setSortable(true).setKey("panelist");
        grid.addColumn(mt -> mt.getSurvey() != null ? mt.getSurvey().getName() : null)
            .setHeader("Encuesta").setAutoWidth(true).setSortable(true).setKey("surveyName");

        // Append the filter row
        HeaderRow filterRow = grid.appendHeaderRow();

        // Create and assign filters to the header row using column keys
        TextField idFilter = createTextFieldFilter("id_filter_key", MessageTask::getId); // Changed filterKey to avoid conflict with column key if map keys are column keys
        filterRow.getCell(grid.getColumnByKey("id")).setComponent(idFilter);

        ComboBox<JobType> jobTypeComboBox = createComboBoxFilter("jobType_filter_key", JobType.class, JobType.values(), MessageTask::getJobType);
        filterRow.getCell(grid.getColumnByKey("jobType")).setComponent(jobTypeComboBox);

        DatePicker createdDatePicker = createDatePickerFilter("createdDate_filter_key");
        filterRow.getCell(grid.getColumnByKey("created")).setComponent(createdDatePicker);

        ComboBox<MessageTaskStatus> statusComboBox = createComboBoxFilter("status_filter_key", MessageTaskStatus.class, MessageTaskStatus.values(), MessageTask::getStatus);
        filterRow.getCell(grid.getColumnByKey("status")).setComponent(statusComboBox);

        TextField participationIdFilter = createTextFieldFilter("participationId_filter_key", mt -> mt.getSurveyPanelistParticipation() != null ? mt.getSurveyPanelistParticipation().getId() : "");
        filterRow.getCell(grid.getColumnByKey("panelist")).setComponent(participationIdFilter);

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
                activeFilters.put(filterKey, (SerializablePredicate<MessageTask>) task -> {
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
                activeFilters.put(filterKey, (SerializablePredicate<MessageTask>) task -> {
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
                activeFilters.put(filterKey, (SerializablePredicate<MessageTask>) task ->
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

        SerializablePredicate<MessageTask> combinedFilter = activeFilters.values().stream()
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
        List<MessageTask> messageTasks = messageTaskService.findAll();
        gridListDataView = grid.setItems(messageTasks);
        // Re-apply filters when data is loaded, if any filters were set previously
        // This is more relevant if filters are persisted or set before data loading.
        // For now, filters are applied on value change.
    }
}
