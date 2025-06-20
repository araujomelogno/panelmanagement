package uy.com.equipos.panelmanagement.views.propierties;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistPropertyCode;
import uy.com.equipos.panelmanagement.data.PropertyType;
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
    private ComboBox<PropertyType> typeFilterCombo = new ComboBox<>();

    private TextField name;
    private ComboBox<PropertyType> type;

    private Grid<PanelistPropertyCode> codesGrid;
    private TextField newCodeValueField;
    private TextField newCodeDescriptionField;
    private Button addCodeButton;
    private VerticalLayout codesManagementSection;

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");
    private Button deleteButton;
    private Button nuevaPropiedadButton;

    private final BeanValidationBinder<PanelistProperty> binder;

    private PanelistProperty panelistProperty; // Instance variable to hold the currently edited property
    private boolean creatingNew = false; // Flag to indicate if a new property is being created

    private final PanelistPropertyService panelistPropertyService;

    public PropertiesView(PanelistPropertyService panelistPropertyService) {
        this.panelistPropertyService = panelistPropertyService;
        addClassNames("propierties-view");

        deleteButton = new Button("Eliminar");
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        deleteButton.addClickListener(e -> onDeleteClicked());

        grid.addColumn(PanelistProperty::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
        grid.addColumn(PanelistProperty::getType).setHeader("Tipo").setKey("type").setAutoWidth(true);
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
            this.panelistProperty = new PanelistProperty(); // Use the class member
            this.creatingNew = true; // Set flag
            populateForm(this.panelistProperty);
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
            PropertyType typeVal = typeFilterCombo.getValue();
            return panelistPropertyService.list(VaadinSpringDataHelpers.toSpringPageRequest(query), nameVal, typeVal)
                    .stream();
        });

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                this.creatingNew = false; // Reset flag
                // When an item is selected from the grid, populateForm will use this selected item.
                // this.panelistProperty will be updated in populateForm.
                populateForm(event.getValue()); // Ensure populateForm is called to set this.panelistProperty
                editorLayoutDiv.setVisible(true);
                UI.getCurrent().navigate(String.format(PANELISTPROPERTY_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                this.creatingNew = false; // Reset flag
                clearForm();
                UI.getCurrent().navigate(PropertiesView.class);
            }
        });

        binder = new BeanValidationBinder<>(PanelistProperty.class);
        binder.forField(type).asRequired("Tipo es requerido").bind(PanelistProperty::getType, PanelistProperty::setType);
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
            this.creatingNew = false; // Reset flag
        });

        save.addClickListener(e -> {
            try {
                if (this.panelistProperty == null) { // Should ideally not happen if logic is correct
                    this.panelistProperty = new PanelistProperty();
                }
                binder.writeBean(this.panelistProperty);
                PanelistProperty savedProperty = panelistPropertyService.save(this.panelistProperty);
                this.creatingNew = false; // Reset flag
                // After saving, this.panelistProperty should ideally be the savedProperty or null if clearing form.
                // The current clearForm() calls populateForm(null), which sets this.panelistProperty to null.
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
            this.creatingNew = false; // Editing an existing property
            Optional<PanelistProperty> panelistPropertyFromBackend = panelistPropertyService.get(panelistPropertyId.get());
            if (panelistPropertyFromBackend.isPresent()) {
                populateForm(panelistPropertyFromBackend.get());
                editorLayoutDiv.setVisible(true);
            } else {
                Notification.show(String.format("La propiedad de panelista solicitada no fue encontrada, ID = %s", panelistPropertyId.get()), 3000, Notification.Position.BOTTOM_START);
                refreshGrid();
                if (editorLayoutDiv != null) editorLayoutDiv.setVisible(false);
                event.forwardTo(PropertiesView.class);
            }
        } else { // No ID in URL
            // If creatingNew is true, it means "Nueva Propiedad" button was clicked.
            // The form is already set up by its click listener (this.panelistProperty = new PanelistProperty(); populateForm(...)).
            // So, do not clear the form if creatingNew is true.
            if (!this.creatingNew) {
                clearForm(); // Clears this.panelistProperty and hides editor
                 if (editorLayoutDiv != null) editorLayoutDiv.setVisible(false);
            }
            // If creatingNew is true, editor should be visible and form populated with a new bean.
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

        formLayout.add(name, type);
        editorDiv.add(formLayout);

        codesManagementSection = new VerticalLayout();
        codesManagementSection.setClassName("codes-management-section");
        codesManagementSection.setPadding(false);
        codesManagementSection.setSpacing(true);

        codesGrid = new Grid<>(PanelistPropertyCode.class, false);
        codesGrid.setClassName("codes-grid");
        codesGrid.addColumn(PanelistPropertyCode::getCode).setHeader("Código").setAutoWidth(true);
        codesGrid.addColumn(PanelistPropertyCode::getDescription).setHeader("Descripción").setAutoWidth(true);
        codesGrid.addComponentColumn(code -> new Button(VaadinIcon.TRASH.create(), click -> {
            // Use this.panelistProperty as the source of truth
            if (this.panelistProperty != null) {
                this.panelistProperty.removeCode(code);
                codesGrid.setItems(this.panelistProperty.getCodes());
            }
        })).setHeader("Acciones").setAutoWidth(true);
        codesGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);

        newCodeValueField = new TextField("Valor del Código");
        newCodeDescriptionField = new TextField("Descripción del Código");
        addCodeButton = new Button("Añadir Código", VaadinIcon.PLUS.create(), e -> addCodeAction());
        addCodeButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        HorizontalLayout newCodeLayout = new HorizontalLayout(newCodeValueField, newCodeDescriptionField, addCodeButton);
        newCodeLayout.setAlignItems(Alignment.BASELINE);
        newCodeLayout.setSpacing(true);

        codesManagementSection.add(new H2("Códigos"), codesGrid, newCodeLayout);
        editorDiv.add(codesManagementSection);
        codesManagementSection.setVisible(false); // Initially hidden

        type.addValueChangeListener(event -> {
            PropertyType selectedType = event.getValue();
            PanelistProperty currentBeanToConsider = this.panelistProperty; // Use instance variable

            // Update the type on our working bean instance directly if it's bound
            if (currentBeanToConsider != null) {
                 // This ensures that if type is changed for a new entity, this.panelistProperty has the correct type
                currentBeanToConsider.setType(selectedType);
            }

            boolean isPropertyAvailable = (currentBeanToConsider != null);
            boolean isTypeCodigo = PropertyType.CODIGO.equals(selectedType);

            boolean enableCodeControls = isPropertyAvailable && isTypeCodigo;
            boolean showCodesSection = isTypeCodigo; // Section visibility only depends on type

            codesManagementSection.setVisible(showCodesSection);

            if (newCodeValueField != null) newCodeValueField.setEnabled(enableCodeControls);
            if (newCodeDescriptionField != null) newCodeDescriptionField.setEnabled(enableCodeControls);
            if (addCodeButton != null) addCodeButton.setEnabled(enableCodeControls);

            if (codesGrid != null) {
                if (showCodesSection && isPropertyAvailable) {
                    if (currentBeanToConsider.getCodes() == null) {
                        currentBeanToConsider.setCodes(new ArrayList<>());
                    }
                    codesGrid.setItems(currentBeanToConsider.getCodes());
                } else {
                    codesGrid.setItems(new ArrayList<>());
                }
            }
        });

        createButtonLayout(editorLayoutDiv);
        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void addCodeAction() {
        // Use this.panelistProperty as the source of truth
        PanelistProperty currentProperty = this.panelistProperty;

        if (currentProperty == null) {
            Notification.show("Por favor, seleccione o guarde la propiedad principal antes de añadir códigos.", 3000, Notification.Position.MIDDLE);
            return;
        }

        String codeValue = newCodeValueField.getValue();
        if (codeValue == null || codeValue.trim().isEmpty()) {
            Notification.show("El valor del código no puede estar vacío.", 3000, Notification.Position.MIDDLE);
            newCodeValueField.focus();
            return;
        }

        PanelistPropertyCode newCode = new PanelistPropertyCode();
        newCode.setCode(codeValue.trim());
        newCode.setDescription(newCodeDescriptionField.getValue() != null ? newCodeDescriptionField.getValue().trim() : null);

        if (currentProperty.getCodes() == null) {
            currentProperty.setCodes(new ArrayList<>());
        }
        currentProperty.addCode(newCode);

        if (PropertyType.CODIGO.equals(currentProperty.getType())) { // Check type from currentProperty
             codesGrid.setItems(currentProperty.getCodes());
        }

        newCodeValueField.clear();
        newCodeDescriptionField.clear();
        newCodeValueField.focus();
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, deleteButton, cancel);
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
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
        headerRow.getCell(grid.getColumnByKey("type")).setComponent(typeFilterCombo);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void populateForm(PanelistProperty value) {
        this.panelistProperty = value; // Set the instance variable
        // If value is null (e.g. when clearing form), this.panelistProperty becomes null.
        // If value is a new PanelistProperty(), this.panelistProperty is that new instance.
        // If value is an existing entity, this.panelistProperty is that existing instance.

        binder.readBean(this.panelistProperty); // Bind the form to the current instance variable

        if (deleteButton != null) {
            deleteButton.setEnabled(value != null && value.getId() != null);
        }

        boolean isPropertyAvailable = (this.panelistProperty != null);
        PropertyType currentType = isPropertyAvailable ? this.panelistProperty.getType() : null;
        boolean isTypeCodigo = PropertyType.CODIGO.equals(currentType);

        boolean enableCodeControls = isPropertyAvailable && isTypeCodigo;
        boolean showCodesSection = isTypeCodigo; // Section visibility only depends on type

        codesManagementSection.setVisible(showCodesSection);

        if (newCodeValueField != null) newCodeValueField.setEnabled(enableCodeControls);
        if (newCodeDescriptionField != null) newCodeDescriptionField.setEnabled(enableCodeControls);
        if (addCodeButton != null) addCodeButton.setEnabled(enableCodeControls);

        if (codesGrid != null) {
            if (showCodesSection && isPropertyAvailable) {
                codesGrid.setItems(this.panelistProperty.getCodes() != null ? this.panelistProperty.getCodes() : new ArrayList<>());
            } else {
                codesGrid.setItems(new ArrayList<>());
            }
        }
    }

    private void clearForm() {
        populateForm(null); // This sets this.panelistProperty to null and updates binder and UI state
        // this.creatingNew = false; // Already handled by cancel and grid selection listeners
        if (editorLayoutDiv != null) {
            editorLayoutDiv.setVisible(false);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(false);
        }
        if (type != null) { // type is a form field
            type.clear(); // This should trigger its listener, which will use the now-null this.panelistProperty
        }
        // Redundant calls to setEnabled(false) if populateForm(null) does its job, but safe.
        if (newCodeValueField != null) {
            newCodeValueField.clear();
            newCodeValueField.setEnabled(false);
        }
        if (newCodeDescriptionField != null) {
            newCodeDescriptionField.clear();
            newCodeDescriptionField.setEnabled(false);
        }
        if (addCodeButton != null) addCodeButton.setEnabled(false);
    }

    private void onDeleteClicked() {
        // Use this.panelistProperty
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
                this.creatingNew = false; // Reset flag
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
