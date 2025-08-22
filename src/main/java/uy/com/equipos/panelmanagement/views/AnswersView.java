package uy.com.equipos.panelmanagement.views;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import uy.com.equipos.panelmanagement.data.AlchemerAnswer;
import uy.com.equipos.panelmanagement.services.AnswerService; // Asumiendo que existirá un AnswerService

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@PageTitle("Respuestas")
@Route(value = "answers", layout = MainLayout.class)
@RolesAllowed("ADMIN") // O el rol que corresponda
public class AnswersView extends Div implements HasComponents, HasStyle {

    private Grid<AlchemerAnswer> grid = new Grid<>(AlchemerAnswer.class, false);
    private GridListDataView<AlchemerAnswer> gridListDataView;
    private final Map<String, SerializablePredicate<AlchemerAnswer>> activeFilters = new HashMap<>();

    private final AnswerService answerService;

    @Autowired
    public AnswersView(AnswerService answerService) {
        this.answerService = answerService;
        addClassNames("answers-view");
        setSizeFull();
        configureGrid();
        loadGridData();
        add(grid);
    }

    private void configureGrid() {
        // Se elimina la columna Id
        // grid.addColumn(Answer::getId).setHeader("Id").setAutoWidth(true).setSortable(true).setKey("id");
        grid.addColumn(AlchemerAnswer::getQuestion).setHeader("Pregunta").setAutoWidth(true).setSortable(true).setKey("question");
        //grid.addColumn(Answer::getQuestionCode).setHeader("Código Pregunta").setAutoWidth(true).setSortable(true).setKey("questionCode");
        grid.addColumn(AlchemerAnswer::getAnswer).setHeader("Respuesta").setAutoWidth(true).setSortable(true).setKey("answer");
        grid.addColumn(answer -> answer.getSurveyPanelistParticipation() != null && answer.getSurveyPanelistParticipation().getPanelist() != null ?
                        answer.getSurveyPanelistParticipation().getPanelist().getFullName() : null)
                .setHeader("Panelista").setAutoWidth(true).setSortable(true).setKey("panelist");
        grid.addColumn(answer -> answer.getSurveyPanelistParticipation() != null && answer.getSurveyPanelistParticipation().getSurvey() != null ?
                        answer.getSurveyPanelistParticipation().getSurvey().getName() : null)
                .setHeader("Encuesta").setAutoWidth(true).setSortable(true).setKey("surveyName");


        HeaderRow filterRow = grid.appendHeaderRow();

        // Se elimina el filtro de Id
        // TextField idFilter = createTextFieldFilter("id_filter", Answer::getId);
        // filterRow.getCell(grid.getColumnByKey("id")).setComponent(idFilter);

        TextField questionFilter = createTextFieldFilter("question_filter", AlchemerAnswer::getQuestion);
        filterRow.getCell(grid.getColumnByKey("question")).setComponent(questionFilter);

//        TextField questionCodeFilter = createTextFieldFilter("questionCode_filter", Answer::getQuestionCode);
//        filterRow.getCell(grid.getColumnByKey("questionCode")).setComponent(questionCodeFilter);

        TextField answerFilter = createTextFieldFilter("answer_filter", AlchemerAnswer::getAnswer);
        filterRow.getCell(grid.getColumnByKey("answer")).setComponent(answerFilter);

        TextField panelistFilter = createTextFieldFilter("panelist_filter", answer ->
            answer.getSurveyPanelistParticipation() != null && answer.getSurveyPanelistParticipation().getPanelist() != null ?
            answer.getSurveyPanelistParticipation().getPanelist().getFullName() : "");
        filterRow.getCell(grid.getColumnByKey("panelist")).setComponent(panelistFilter);

        TextField surveyNameFilter = createTextFieldFilter("surveyName_filter", answer ->
            answer.getSurveyPanelistParticipation() != null && answer.getSurveyPanelistParticipation().getSurvey() != null ?
            answer.getSurveyPanelistParticipation().getSurvey().getName() : "");
        filterRow.getCell(grid.getColumnByKey("surveyName")).setComponent(surveyNameFilter);

        grid.setSizeFull();
    }

    private TextField createTextFieldFilter(String filterKey, ValueProvider<AlchemerAnswer, ?> valueProvider) {
        TextField filterField = new TextField();
        filterField.setWidthFull();
        filterField.setPlaceholder("Filtrar...");
        filterField.setClearButtonVisible(true);
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.addValueChangeListener(event -> {
            String filterValue = event.getValue() != null ? event.getValue().toLowerCase() : "";
            if (filterValue.isEmpty()) {
                activeFilters.remove(filterKey);
            } else {
                activeFilters.put(filterKey, (SerializablePredicate<AlchemerAnswer>) answer ->
                        Objects.toString(valueProvider.apply(answer), "").toLowerCase().contains(filterValue)
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
        SerializablePredicate<AlchemerAnswer> combinedFilter = activeFilters.values().stream()
                .reduce(SerializablePredicate::and)
                .orElse(task -> true);
        gridListDataView.setFilter(combinedFilter);
    }

    @FunctionalInterface
    interface ValueProvider<T, R> {
        R apply(T t);
    }

    private void loadGridData() {
        List<AlchemerAnswer> answers = answerService.findAll(); // Asumiendo que AnswerService tiene un método findAll()
        gridListDataView = grid.setItems(answers);
        applyFilters();
    }
}
