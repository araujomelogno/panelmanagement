package uy.com.equipos.panelmanagement.views.dialogs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPropertyMatching;
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;
import uy.com.equipos.panelmanagement.services.SurveyPropertyMatchingService;

public class VincularPropiedadesDialog extends Dialog {

    private final Survey survey;
    private final Grid<PanelistProperty> grid = new Grid<>(PanelistProperty.class, false);
    private final PanelistPropertyService panelistPropertyService;
    private final SurveyPropertyMatchingService surveyPropertyMatchingService;
    private final Map<PanelistProperty, Checkbox> checkboxMap = new HashMap<>();
    private final Map<PanelistProperty, TextField> textFieldMap = new HashMap<>();

    public VincularPropiedadesDialog(Survey survey, PanelistPropertyService panelistPropertyService,
            SurveyPropertyMatchingService surveyPropertyMatchingService) {
        this.survey = survey;
        this.panelistPropertyService = panelistPropertyService;
        this.surveyPropertyMatchingService = surveyPropertyMatchingService;
        setHeaderTitle("Vincular Propiedades para: " + survey.getName());
        setWidth("80%");
        setHeight("70%");

        VerticalLayout dialogLayout = new VerticalLayout(grid);
        dialogLayout.setSizeFull();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        add(dialogLayout);

        Button closeButton = new Button("Cerrar", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button saveButton = new Button("Guardar", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(closeButton, saveButton);

        setupGrid();
        loadPanelistProperties();
    }

    private void setupGrid() {
    	 Grid.Column<PanelistProperty> nameColumn = grid.addColumn(PanelistProperty::getName).setHeader("Nombre").setSortable(true).setFlexGrow(1);

         TextField nameFilter = new TextField();
         nameFilter.setPlaceholder("Filtrar...");
         nameFilter.setClearButtonVisible(true);
         nameFilter.addValueChangeListener(e -> {
             // Logic to update the grid will be here
         });

         grid.getHeaderRows().clear();
         HeaderRow headerRow = grid.appendHeaderRow();
         headerRow.getCell(nameColumn).setComponent(nameFilter);

        grid.addComponentColumn(property -> {
            Checkbox checkbox = new Checkbox();
            Optional<SurveyPropertyMatching> match = surveyPropertyMatchingService.findBySurveyAndProperty(survey, property);
            match.ifPresent(surveyPropertyMatching -> checkbox.setValue(true));
            checkboxMap.put(property, checkbox);
            return checkbox;
        }).setHeader("Seleccionar").setFlexGrow(0);

//        grid.addColumn(PanelistProperty::getName).setHeader("Nombre").setSortable(true).setFlexGrow(1);

        grid.addComponentColumn(property -> {
            TextField textField = new TextField();
            Optional<SurveyPropertyMatching> match = surveyPropertyMatchingService.findBySurveyAndProperty(survey, property);
            match.ifPresent(surveyPropertyMatching -> textField.setValue(surveyPropertyMatching.getQuestionLabel()));
            textFieldMap.put(property, textField);
            return textField;
        }).setHeader("Etiqueta").setFlexGrow(1);
    }

    private void loadPanelistProperties() {
        grid.setItems(panelistPropertyService.findAll());
    }

    private void save() {
        for (Map.Entry<PanelistProperty, Checkbox> entry : checkboxMap.entrySet()) {
            PanelistProperty property = entry.getKey();
            Checkbox checkbox = entry.getValue();
            TextField textField = textFieldMap.get(property);

            if (checkbox.getValue()) {
                Optional<SurveyPropertyMatching> existingMatch = surveyPropertyMatchingService.findBySurveyAndProperty(survey, property);
                SurveyPropertyMatching match = existingMatch.orElseGet(SurveyPropertyMatching::new);
                match.setSurvey(survey);
                match.setProperty(property);
                match.setQuestionLabel(textField.getValue());
                surveyPropertyMatchingService.save(match);
            } else {
                surveyPropertyMatchingService.findBySurveyAndProperty(survey, property)
                        .ifPresent(match -> surveyPropertyMatchingService.delete(match.getId()));
            }
        }
        close();
    }
}
