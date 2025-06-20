package uy.com.equipos.panelmanagement.views.propierties;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox; // Added import
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout; // Added import
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField; // Added import (already present but good to confirm)
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import org.springframework.dao.DataIntegrityViolationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList; // Added import
import java.util.List; // Added import
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCode; // Added import
import uy.com.equipos.panelmanagement.data.PropertyType; // Added import
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;

@PageTitle("Propiedades de panelistas")
@Route("properties/:panelistPropertyID?/:action?(edit)")
@Menu(order = 2, icon = LineAwesomeIconUrl.EDIT)
@PermitAll
public class PropertiesView extends Div implements BeforeEnterObserver {

    private final String PANELISTPROPERTY_ID = "panelistPropertyID";
    private final String PANELISTPROPERTY_EDIT_ROUTE_TEMPLATE = "properties/%s/edit";

    private final Grid<PanelistProperty> grid = new Grid<>(PanelistProperty.class, false);
    private Div editorLayoutDiv;

    private TextField nameFilter = new TextField();
    private ComboBox<PropertyType> typeFilterCombo = new ComboBox<>(); // Changed for consistency if filtering by enum

    private TextField name;
    // private TextField type; // Replaced by ComboBox
    private ComboBox<PropertyType> type; // New ComboBox for property type

    // Fields for managing codes
    private Grid<PanelistPropertyCode> codesGrid;
    private TextField newCodeValueField;
    private TextField newCodeDescriptionField;
    private Button addCodeButton;
    private VerticalLayout codesManagementSection; // Changed to VerticalLayout for better structure

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");
    private Button deleteButton;
    private Button nuevaPropiedadButton;

    private final BeanValidationBinder<PanelistProperty> binder;

    private PanelistProperty panelistProperty;

    private final PanelistPropertyService panelistPropertyService;

    public PropertiesView(PanelistPropertyService panelistPropertyService) {
        this.panelistPropertyService = panelistPropertyService;
        addClassNames("propierties-view");

        deleteButton = new Button("Eliminar");
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        deleteButton.addClickListener(e -> onDeleteClicked());

        grid.addColumn(PanelistProperty::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
        grid.addColumn(PanelistProperty::getType).setHeader("Tipo").setKey("type").setAutoWidth(true); // This will now display enum toString
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        SplitLayout splitLayout = new SplitLayout();
        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        nuevaPropiedadButton = new Button("Nueva Propiedad");
        nuevaPropiedadButton.getStyle().set("margin-left", "18px");
        
        VerticalLayout mainLayout = new VerticalLayout(nuevaPropiedadButton, splitLayout);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);

        add(mainLayout);
        if (editorLayoutDiv != null) {
            editorLayoutDiv.setVisible(false);
        }

        nuevaPropiedadButton.addClickListener(click -> {
            grid.asSingleSelect().clear();
            populateForm(new PanelistProperty());
            if (editorLayoutDiv != null) {
                editorLayoutDiv.setVisible(true);
            }
            if (name != null) {
                name.focus();
            }
        });

        nameFilter.setPlaceholder("Filtrar por Nombre");
        typeFilterCombo.setPlaceholder("Filtrar por Tipo");
        typeFilterCombo.setItems(PropertyType.values());


        nameFilter.addValueChangeListener(e -> refreshGridData());
        typeFilterCombo.addValueChangeListener(e -> refreshGridData());


        grid.setItems(query -> {
            String nameVal = nameFilter.getValue();
            PropertyType typeVal = typeFilterCombo.getValue(); // Get value from ComboBox

            // Pass PropertyType to service method
            return panelistPropertyService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), nameVal, typeVal)
                    .stream();
        });
        
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editorLayoutDiv.setVisible(true);
                UI.getCurrent().navigate(String.format(PANELISTPROPERTY_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(PropertiesView.class);
            }
        });

        binder = new BeanValidationBinder<>(PanelistProperty.class);
        // type field is now a ComboBox
        binder.forField(type).asRequired("Tipo es requerido").bind(PanelistProperty::getType, PanelistProperty::setType);
        binder.bindInstanceFields(this); // Binds 'name' automatically

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.panelistProperty == null) {
                    this.panelistProperty = new PanelistProperty();
                }
                // Codes are managed directly in the list of panelistProperty
                binder.writeBean(this.panelistProperty);
                panelistPropertyService.save(this.panelistProperty);
                clearForm();
                refreshGrid();
                Notification.show("Datos actualizados");
                UI.getCurrent().navigate(PropertiesView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error al actualizar los datos. Otro usuario modificó el registro mientras usted realizaba cambios.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Fallo al actualizar los datos. Verifique nuevamente que todos los valores sean válidos");
            }
        });
        save.addClickShortcut(Key.ENTER);
    }
    
    private void refreshGridData() {
        grid.getDataProvider().refreshAll();
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> panelistPropertyId = event.getRouteParameters().get(PANELISTPROPERTY_ID).map(Long::parseLong);
        if (panelistPropertyId.isPresent()) {
            Optional<PanelistProperty> panelistPropertyFromBackend = panelistPropertyService.get(panelistPropertyId.get());
            if (panelistPropertyFromBackend.isPresent()) {
                populateForm(panelistPropertyFromBackend.get());
                editorLayoutDiv.setVisible(true);
            } else {
                Notification.show(String.format("La propiedad de panelista solicitada no fue encontrada, ID = %s", panelistPropertyId.get()), 3000, Notification.Position.BOTTOM_START);
                refreshGrid();
                if (editorLayoutDiv != null) {
                    editorLayoutDiv.setVisible(false);
                }
                event.forwardTo(PropertiesView.class);
            }
        } else {
            clearForm();
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        name = new TextField("Nombre");
        type = new ComboBox<>("Tipo");
        type.setItems(PropertyType.values());
        // type.setItemLabelGenerator(PropertyType::name); // Or a more user-friendly representation

        formLayout.add(name, type);
        editorDiv.add(formLayout);

        // Initialize codes management components
        codesManagementSection = new VerticalLayout();
        codesManagementSection.setClassName("codes-management-section");
        codesManagementSection.setPadding(false);
        codesManagementSection.setSpacing(true); // Add some space between elements
        codesManagementSection.setSizeFull(); // Allow codesManagementSection to fill available space

        codesGrid = new Grid<>(PanelistPropertyCode.class, false);
        codesGrid.setClassName("codes-grid");
        //codesGrid.setHeight("100%"); // Allow codesGrid to take full height within its container
        codesGrid.addComponentColumn(code -> new Button(VaadinIcon.TRASH.create(), click -> {
            PanelistProperty property = binder.getBean();
            if (property != null) {
                property.removeCode(code); // Use helper method
                codesGrid.setItems(property.getCodes()); // Refresh grid
            }
        })).setHeader("Acciones").setAutoWidth(true);
        codesGrid.addColumn(PanelistPropertyCode::getCode).setHeader("Código").setAutoWidth(true);
        codesGrid.addColumn(PanelistPropertyCode::getDescription).setHeader("Descripción").setAutoWidth(true);
        codesGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);


        newCodeValueField = new TextField("Valor del Código");
        newCodeDescriptionField = new TextField("Descripción del Código");
        addCodeButton = new Button("Añadir Código", VaadinIcon.PLUS.create(), event -> addCodeAction());
        addCodeButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        // Changed to VerticalLayout, components are added in the desired order.
        VerticalLayout newCodeLayout = new VerticalLayout(newCodeValueField, newCodeDescriptionField, addCodeButton);
        codesGrid.setMaxHeight("240px");
        
        // Corrected order of components in codesManagementSection
        codesManagementSection.add(newCodeLayout, codesGrid);
        
        editorDiv.add(codesManagementSection); // Add section to editor

        // Initial visibility
        codesManagementSection.setVisible(false);

        // Event handling for type ComboBox
        type.addValueChangeListener(event -> {
            PropertyType selectedType = event.getValue(); // Use the event's value directly
            PanelistProperty currentBean = binder.getBean();

            boolean isPropertyBound = (currentBean != null);
            boolean isTypeCodigo = PropertyType.CODIGO.equals(selectedType);

            boolean enableCodeControls = isPropertyBound && isTypeCodigo;
            boolean showCodesSection = isTypeCodigo; // Section visibility depends only on type being CODIGO

            codesManagementSection.setVisible(showCodesSection);
            
            // Enable/disable individual controls
            if (newCodeValueField != null) newCodeValueField.setEnabled(enableCodeControls);
            if (newCodeDescriptionField != null) newCodeDescriptionField.setEnabled(enableCodeControls);
            if (addCodeButton != null) addCodeButton.setEnabled(enableCodeControls);
            
            // Grid visibility is part of codesManagementSection, but items depend on bean
            if (codesGrid != null) {
                if (showCodesSection && isPropertyBound) { // Show grid content only if section visible AND bean bound
                    if (currentBean.getCodes() == null) {
                        currentBean.setCodes(new ArrayList<>());
                    }
                    codesGrid.setItems(currentBean.getCodes());
                } else {
                    codesGrid.setItems(new ArrayList<>()); // Clear grid if not CODIGO or no bean
                }
            }
        });
        
        createButtonLayout(editorLayoutDiv);
        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void addCodeAction() {
        PanelistProperty currentProperty = binder.getBean();

        if (currentProperty == null) {
            Notification.show("Por favor, seleccione o guarde la propiedad principal antes de añadir códigos. ", 3000, Notification.Position.MIDDLE);
            return;
        }

        String codeValue = newCodeValueField.getValue(); 
        if (codeValue == null || codeValue.trim().isEmpty()) {
            Notification.show("El valor del código no puede estar vacío.", 3000, Notification.Position.MIDDLE);
            newCodeValueField.focus(); // Optional: set focus back to the field
            return;
        }
 
        PanelistPropertyCode newCode = new PanelistPropertyCode();
        newCode.setCode(codeValue.trim());
        newCode.setDescription(newCodeDescriptionField.getValue() != null ? newCodeDescriptionField.getValue().trim() : null); // Trim description too

        // Ensure codes list is initialized - addCode helper should handle this, but defensive check is fine
        if (currentProperty.getCodes() == null) {
            currentProperty.setCodes(new ArrayList<>());
        }
        currentProperty.addCode(newCode); // Uses the helper method

        // Refresh grid only if it's visible (i.e., type is CODIGO)
        if (PropertyType.CODIGO.equals(type.getValue())) {
             codesGrid.setItems(currentProperty.getCodes());
        }
       
        newCodeValueField.clear();
        newCodeDescriptionField.clear();
        newCodeValueField.focus(); // Optional: set focus for next entry
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, deleteButton, cancel);
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END); // Align buttons to the right
        buttonLayout.setWidthFull(); 
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(grid.getColumnByKey("name")).setComponent(nameFilter);
        // headerRow.getCell(grid.getColumnByKey("type")).setComponent(typeFilter); // Old text filter
        headerRow.getCell(grid.getColumnByKey("type")).setComponent(typeFilterCombo); // New combo filter
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void populateForm(PanelistProperty value) {
        this.panelistProperty = value;
        binder.readBean(this.panelistProperty);

        if (deleteButton != null) {
            deleteButton.setEnabled(value != null && value.getId() != null);
        }
        
        // Handle visibility and content of codes section
        boolean isExistingProperty = value != null && value.getId() != null; // Existing if ID is present
        boolean isNewUnsavedProperty = value != null && value.getId() == null; // New but not yet saved

        if (value != null) {
            boolean isCodigo = PropertyType.CODIGO.equals(value.getType());
            codesManagementSection.setVisible(isCodigo);
            
            // Enable controls only if property type is CODIGO and property is not null (selected or new)
            boolean enableCodeControls = isCodigo && (value != null); 
            newCodeValueField.setEnabled(enableCodeControls);
            newCodeDescriptionField.setEnabled(enableCodeControls);
            addCodeButton.setEnabled(enableCodeControls);
            // codesGrid.setEnabled(enableCodeControls); // Grid itself might not need to be disabled, just its content managed

            if (isCodigo) {
                codesGrid.setItems(value.getCodes() != null ? value.getCodes() : new ArrayList<>());
            } else {
                codesGrid.setItems(new ArrayList<>());
            }
        } else { // Value is null (form is being cleared)
            codesManagementSection.setVisible(false);
            codesGrid.setItems(new ArrayList<>());
            newCodeValueField.setEnabled(false);
            newCodeDescriptionField.setEnabled(false);
            addCodeButton.setEnabled(false);
            // codesGrid.setEnabled(false);
        }
    }

    private void clearForm() {
        populateForm(null); // This will also hide codesManagementSection and clear grid
        if (editorLayoutDiv != null) {
            editorLayoutDiv.setVisible(false);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(false);
        }
        // Explicitly ensure type combo is cleared and code fields too
        if (type != null) {
            type.clear(); // This will trigger its value change listener which should handle disabling code fields
        }
        // The populateForm(null) call already handles hiding the section and clearing the grid.
        // The type.clear() above, through its listener, should ensure controls are disabled.
        // However, an explicit disable here is safer if the listener logic changes or has complex conditions.
        newCodeValueField.setEnabled(false);
        newCodeDescriptionField.setEnabled(false);
        addCodeButton.setEnabled(false);
        // codesGrid.setEnabled(false); // If grid itself needs disabling

        if (newCodeValueField != null) newCodeValueField.clear(); // Still good to clear values
        if (newCodeDescriptionField != null) newCodeDescriptionField.clear();
    }

    private void onDeleteClicked() {
        if (this.panelistProperty == null || this.panelistProperty.getId() == null) {
            Notification.show("No hay propiedad seleccionada para eliminar.", 3000, Notification.Position.MIDDLE);
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar Eliminación");
        dialog.setText("¿Está seguro de que desea eliminar la propiedad '" + this.panelistProperty.getName() + "'?");
        
        dialog.setConfirmText("Eliminar");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelText("Cancelar");

        dialog.addConfirmListener(event -> {
            try {
                panelistPropertyService.delete(this.panelistProperty.getId());
                clearForm();
                refreshGrid();
                Notification.show("Propiedad eliminada correctamente.", 3000, Notification.Position.BOTTOM_START);
                UI.getCurrent().navigate(PropertiesView.class);
            } catch (DataIntegrityViolationException ex) {
                Notification.show("No se puede eliminar la propiedad. Es posible que esté siendo referenciada por otras entidades.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                Notification.show("Ocurrió un error al intentar eliminar la propiedad: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }
}
