package uy.com.equipos.panelmanagement.views.panels;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue; // Corrected import
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox; // Added import
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment; // Added
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCode;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCodeRepository; // Corrected path
import uy.com.equipos.panelmanagement.data.PropertyType; // Corrected import
import uy.com.equipos.panelmanagement.services.PanelService; // Added
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;
import uy.com.equipos.panelmanagement.services.PanelistService;

public class PanelistPropertyFilterDialog extends Dialog {

    private final PanelistPropertyService panelistPropertyService;
    private final PanelistPropertyCodeRepository panelistPropertyCodeRepository;
    private final PanelService globalPanelService; // Renamed for clarity from panelService to globalPanelService
    private final PanelistService panelistService;
    private final Panel currentPanel;

    private Grid<PanelistProperty> propertiesGrid;
    private Button cancelButton = new Button("Cancelar");
    private Button searchButton = new Button("Buscar");

    // To store references to the editor components and checkboxes
    private Map<PanelistProperty, Component> propertyEditorMap = new HashMap<>();
    private Map<PanelistProperty, Checkbox> propertyCheckboxMap = new HashMap<PanelistProperty, Checkbox>();
    private VerticalLayout contentLayout; // Made field

    // Listener para comunicar los filtros seleccionados
    @FunctionalInterface
    public interface SearchListener {
        void onSearch(Map<PanelistProperty, Object> filterCriteria);
    }
    private SearchListener searchListener;


    public PanelistPropertyFilterDialog(
            PanelistPropertyService panelistPropertyService,
            PanelistPropertyCodeRepository panelistPropertyCodeRepository,
            PanelService globalPanelService,
            PanelistService panelistService,
            Panel currentPanel, // Puede ser null si el diálogo se usa fuera del contexto de un Panel específico
            SearchListener searchListener) { // Añadido searchListener
        this.panelistPropertyService = panelistPropertyService;
        this.panelistPropertyCodeRepository = panelistPropertyCodeRepository;
        this.globalPanelService = globalPanelService;
        this.panelistService = panelistService;
        this.currentPanel = currentPanel;
        this.searchListener = searchListener;


        setHeaderTitle("Filtrar Panelistas por Propiedades");
        setWidth("800px"); // Ajustado para más contenido

        contentLayout = new VerticalLayout();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        contentLayout.setAlignItems(Alignment.STRETCH);

        propertiesGrid = new Grid<>(PanelistProperty.class, false);

        // Columna de CheckBox
        propertiesGrid.addComponentColumn(property -> {
            Checkbox checkbox = new Checkbox();
            propertyCheckboxMap.put(property, checkbox);
            return checkbox;
        }).setHeader("Seleccionar").setWidth("120px").setFlexGrow(0);

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
            for (Map.Entry<PanelistProperty, Checkbox> checkboxEntry : propertyCheckboxMap.entrySet()) {
                PanelistProperty prop = checkboxEntry.getKey();
                Checkbox checkbox = checkboxEntry.getValue();

                if (checkbox.getValue()) { // Solo procesar si el checkbox está marcado
                    Component editor = propertyEditorMap.get(prop);
                    Object value = null;

                    if (editor instanceof TextField) { // TextField para NUMERO y TEXTO
                        String textValue = ((TextField) editor).getValue();
                        if (textValue != null && !textValue.isBlank()) {
                            // Si es de tipo NUMERO, intentamos parsear a un tipo numérico o mantener como String validado.
                            // Por ahora, lo mantenemos como String, el servicio se encargará de la conversión/validación.
                            value = textValue;
                        }
                    } else if (editor instanceof DatePicker) {
                        value = ((DatePicker) editor).getValue();
                    } else if (editor instanceof ComboBox) {
                        // Para ComboBox<PanelistPropertyCode>, el valor es PanelistPropertyCode.
                        // El servicio esperará el objeto PanelistPropertyCode o su representación String (ej. el código).
                        // Aquí asumimos que el servicio puede manejar el objeto PanelistPropertyCode directamente si se pasa.
                        // O si se necesita el String, sería ((ComboBox<PanelistPropertyCode>) editor).getValue().getCode().
                        value = ((ComboBox<?>) editor).getValue();
                    }
                    // No hay NumberField, ya que se reemplazó por TextField para NUMERO.

                    if (value != null) {
                        filterCriteria.put(prop, value);
                    }
                }
            }

            if (searchListener != null) {
                searchListener.onSearch(filterCriteria);
                // Si hay un listener, asumimos que el flujo continuará en otro diálogo,
                // por lo que este diálogo de filtro debe cerrarse.
                close();
            }
            // Si no hay searchListener, el diálogo permanecerá abierto (comportamiento anterior
            // cuando PanelistPropertyFilterDialog abría PanelistSelectionDialog directamente).
            // Sin embargo, con el refactor, siempre debería haber un searchListener si se espera una acción.
        });
    }

    // Mantener closeDialog() por si es llamado externamente, aunque el flujo principal ahora cierra internamente.
    public void closeDialog() {
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
                ((TextField) editorComponent).setPlaceholder("Valor de texto...");
                break;
            case FECHA:
                DatePicker datePicker = new DatePicker();
                datePicker.setPlaceholder("dd/MM/yyyy");
                datePicker.setLocale(new Locale("es", "UY"));
                DatePicker.DatePickerI18n dpI18n = new DatePicker.DatePickerI18n();
                dpI18n.setDateFormat("dd/MM/yyyy");
                datePicker.setI18n(dpI18n);
                editorComponent = datePicker;
                break;
            case NUMERO:
                // Usar TextField para números, se puede añadir validación o máscara si es necesario.
                TextField numeroField = new TextField();
                numeroField.setPlaceholder("Valor numérico...");
                // Opcional: añadir un pattern para validación básica de números.
                // numeroField.setPattern("[0-9]*"); // Solo enteros positivos
                // numeroField.setPattern("^-?[0-9]*\\.?[0-9]+$"); // Números decimales, opcionalmente negativos
                editorComponent = numeroField;
                break;
            case CODIGO:
                ComboBox<PanelistPropertyCode> comboBox = new ComboBox<>();
                ((ComboBox<PanelistPropertyCode>) comboBox).setPlaceholder("Seleccione código...");
                List<PanelistPropertyCode> codes = panelistPropertyCodeRepository.findByPanelistProperty(property);
                comboBox.setItems(codes);
                comboBox.setItemLabelGenerator(PanelistPropertyCode::getCode);
                editorComponent = comboBox;
                break;
            default:
                editorComponent = new Span("Tipo no soportado: " + type);
        }
        // Set width for all editor components to fill the cell
        if (editorComponent instanceof HasValue) {
            // getElement() is available on Component, so editorComponent.getElement() is fine.
            // The HasValue check is okay if we might do other HasValue specific things,
            // but for getElement, it's directly on Component.
            editorComponent.getElement().getStyle().set("width", "100%");
        }
        return editorComponent;
    }
}
