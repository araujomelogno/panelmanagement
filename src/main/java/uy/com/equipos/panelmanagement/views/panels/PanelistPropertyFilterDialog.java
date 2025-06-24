package uy.com.equipos.panelmanagement.views.panels;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment; // Added
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField; // Added
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.HasValue; // Corrected import
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCode;
import uy.com.equipos.panelmanagement.data.PropertyType; // Corrected import
import uy.com.equipos.panelmanagement.data.PanelistPropertyCodeRepository; // Corrected path
import uy.com.equipos.panelmanagement.services.PanelService; // Added
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;
import uy.com.equipos.panelmanagement.services.PanelistService;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanelistPropertyFilterDialog extends Dialog {

    private final PanelistPropertyService panelistPropertyService;
    private final PanelistPropertyCodeRepository panelistPropertyCodeRepository;
    private final PanelService globalPanelService; // Renamed for clarity from panelService to globalPanelService
    private final PanelistService panelistService;
    private final Panel currentPanel;

    private Grid<PanelistProperty> propertiesGrid;
    private Button cancelButton = new Button("Cancelar");
    private Button searchButton = new Button("Buscar");

    // To store references to the editor components
    private Map<PanelistProperty, Component> propertyEditorMap = new HashMap<>();
    private VerticalLayout contentLayout; // Made field

    public PanelistPropertyFilterDialog(
            PanelistPropertyService panelistPropertyService,
            PanelistPropertyCodeRepository panelistPropertyCodeRepository,
            PanelService globalPanelService, // Added
            PanelistService panelistService,
            Panel currentPanel) {
        this.panelistPropertyService = panelistPropertyService;
        this.panelistPropertyCodeRepository = panelistPropertyCodeRepository;
        this.globalPanelService = globalPanelService; // Added
        this.panelistService = panelistService;
        this.currentPanel = currentPanel;

        setHeaderTitle("Buscar Panelistas - Paso 1: Filtrar por Propiedades");
        setWidth("600px"); // Set a width for the dialog

        contentLayout = new VerticalLayout(); // Initialize field
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        contentLayout.setAlignItems(Alignment.STRETCH); // Stretch items

        propertiesGrid = new Grid<>(PanelistProperty.class, false);
        propertiesGrid.addColumn(PanelistProperty::getName).setHeader("Propiedad").setFlexGrow(1);
        propertiesGrid.addComponentColumn(this::createEditorComponentAndStore).setHeader("Valor").setFlexGrow(2);

        List<PanelistProperty> properties = panelistPropertyService.findAll();
        if (properties == null || properties.isEmpty()) {
            propertiesGrid.setVisible(false); // Hide grid if no properties
            Span noPropertiesMessage = new Span("No hay propiedades de panelista definidas para filtrar.");
            noPropertiesMessage.getStyle().set("text-align", "center").set("font-style", "italic");
            contentLayout.add(noPropertiesMessage);
            searchButton.setEnabled(false); // Disable search if no properties
        } else {
            propertiesGrid.setItems(properties);
            propertiesGrid.setWidthFull(); // Set width only if visible
            contentLayout.add(propertiesGrid);
        }

        add(contentLayout);

        getFooter().add(cancelButton, searchButton);

        cancelButton.addClickListener(e -> close());
        searchButton.addClickListener(e -> {
            Map<PanelistProperty, Object> filterCriteria = new HashMap<>();
            for (Map.Entry<PanelistProperty, Component> entry : propertyEditorMap.entrySet()) {
                PanelistProperty prop = entry.getKey();
                Component editor = entry.getValue();
                Object value = null;

                if (editor instanceof NumberField) { // Check NumberField first
                    Double numValue = ((NumberField) editor).getValue();
                    if (numValue != null) {
                        value = numValue.toString(); // Convert Double to String
                    }
                } else if (editor instanceof TextField) {
                    String textValue = ((TextField) editor).getValue();
                    if (textValue != null && !textValue.isBlank()) {
                        value = textValue;
                    }
                } else if (editor instanceof DatePicker) {
                    value = ((DatePicker) editor).getValue();
                } else if (editor instanceof ComboBox) {
                    value = ((ComboBox<?>) editor).getValue();
                }
                // Add other types if needed

                if (value != null) { // Ensure value is not null after processing
                    filterCriteria.put(prop, value);
                }
            }

            // Open Step 2 Dialog
            PanelistSelectionDialog selectionDialog = new PanelistSelectionDialog(
                    this.globalPanelService, this.panelistService, currentPanel, filterCriteria, this); // Added 'this'
            selectionDialog.open();
            // close(); // Do not close here anymore, PanelistSelectionDialog will close it
        });
    }

    public void closeDialog() { // Added public method
        this.close();
    }

    private Component createEditorComponentAndStore(PanelistProperty property) {
        Component editor = createEditorComponent(property);
        propertyEditorMap.put(property, editor); // Store the editor
        return editor;
    }

    private Component createEditorComponent(PanelistProperty property) {
        if (property == null || property.getType() == null) {
            return new Span("Propiedad o tipo no definido");
        }

        Component editorComponent;
        // Ensure property.getType() returns PropertyType enum
        PropertyType type = property.getType();
        if (type == null) {
             return new Span("Tipo de propiedad no definido para: " + property.getName());
        }

        switch (type) {
            case TEXTO:
                editorComponent = new TextField();
                break;
            case FECHA:
                editorComponent = new DatePicker();
                break;
            case NUMERO:
                NumberField numField = new NumberField();
                numField.setPreventInvalidInput(true);
                editorComponent = numField;
                break;
            case CODIGO:
                ComboBox<PanelistPropertyCode> comboBox = new ComboBox<>();
                List<PanelistPropertyCode> codes = panelistPropertyCodeRepository.findByPanelistProperty(property);
                comboBox.setItems(codes);
                comboBox.setItemLabelGenerator(PanelistPropertyCode::getDescription);
                editorComponent = comboBox;
                break;
            default:
                editorComponent = new Span("Tipo no soportado: " + type);
        }
        // Set width for all editor components to fill the cell
        if (editorComponent instanceof HasValue) {
            ((HasValue<?, ?>) editorComponent).getElement().getStyle().set("width", "100%");
        }
        return editorComponent;
    }
}
