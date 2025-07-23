package uy.com.equipos.panelmanagement.views;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import jakarta.annotation.security.PermitAll;
import uy.com.equipos.panelmanagement.data.ConfigurationItem;
import uy.com.equipos.panelmanagement.services.ConfigurationItemService;

@PageTitle("Propiedades de sistema")
@Route(value = "configuration-items/:configurationItemID?/:action?(edit)", layout = MainLayout.class)
@PermitAll
public class ConfigurationView extends Div implements BeforeEnterObserver {

    private final String CONFIGURATIONITEM_ID = "configurationItemID";
    private final String CONFIGURATIONITEM_EDIT_ROUTE_TEMPLATE = "configuration-items/%s/edit";

    private final Grid<ConfigurationItem> grid = new Grid<>(ConfigurationItem.class, false);

    private TextField nameFilter = new TextField();
    private TextField valueFilter = new TextField();

    private TextField name;
    private TextArea value;

    private final Button cancel = new Button("Cerrar");
    private final Button save = new Button("Guardar");
    private final Button delete = new Button("Borrar");

    private final BeanValidationBinder<ConfigurationItem> binder;

    private ConfigurationItem configurationItem;

    private final ConfigurationItemService configurationItemService;

    private Div editorLayoutDiv;

    public ConfigurationView(ConfigurationItemService configurationItemService) {
        this.configurationItemService = configurationItemService;
        addClassNames("configuration-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        Button newButton = new Button("Nuevo item de configuración");
        newButton.addClickListener(e -> {
            grid.asSingleSelect().clear();
            populateForm(new ConfigurationItem());
            editorLayoutDiv.setVisible(true);
        });

        // Configure Grid 

		grid.addColumn(ConfigurationItem::getName).setHeader("Nombre").setKey("name").setAutoWidth(true).setSortable(true);
		grid.addColumn(ConfigurationItem::getValue).setHeader("Valor").setKey("value").setAutoWidth(true).setSortable(true);

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        HorizontalLayout buttonLayout = new HorizontalLayout(newButton);
        add(buttonLayout, splitLayout);

        editorLayoutDiv.setVisible(false);

        nameFilter.addValueChangeListener(e -> this.refreshGrid());
        valueFilter.addValueChangeListener(e -> this.refreshGrid());
        grid.setItems(query -> {
            Specification<ConfigurationItem> spec = (root, cbQuery, cb) -> {
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                if (nameFilter.getValue() != null && !nameFilter.getValue().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameFilter.getValue().toLowerCase() + "%"));
                }
                if (valueFilter.getValue() != null && !valueFilter.getValue().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("value")), "%" + valueFilter.getValue().toLowerCase() + "%"));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            return configurationItemService.list(
                VaadinSpringDataHelpers.toSpringPageRequest(query),
                spec).stream();
        });
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(CONFIGURATIONITEM_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(ConfigurationView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(ConfigurationItem.class);

        // Bind fields. This where you'd define e.g. validation rules
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
            editorLayoutDiv.setVisible(false);
        });

        save.addClickListener(e -> {
            try {
                if (this.configurationItem == null) {
                    this.configurationItem = new ConfigurationItem();
                }
                binder.writeBean(this.configurationItem);
                configurationItemService.update(this.configurationItem);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(ConfigurationView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });

        delete.addClickListener(e -> {
            if (this.configurationItem != null && this.configurationItem.getId() != null) {
                configurationItemService.delete(this.configurationItem.getId());
                clearForm();
                refreshGrid();
                Notification.show("ConfigurationItem deleted");
                UI.getCurrent().navigate(ConfigurationView.class);
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> configurationItemId = event.getRouteParameters().get(CONFIGURATIONITEM_ID).map(Long::parseLong);
        if (configurationItemId.isPresent()) {
            Optional<ConfigurationItem> configurationItemFromBackend = configurationItemService.get(configurationItemId.get());
            if (configurationItemFromBackend.isPresent()) {
                populateForm(configurationItemFromBackend.get());
                editorLayoutDiv.setVisible(true);
            } else {
                Notification.show(
                        String.format("The requested configurationItem was not found, ID = %s", configurationItemId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(ConfigurationView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

 
        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        
        name = new TextField("Name");
        value = new TextArea("Value");
        value.setMinRows(8);
        
        
        VerticalLayout layout = new VerticalLayout();
        layout.add(name, value);

        formLayout.add(layout);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        buttonLayout.add(save, delete, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);

        // Add filters
        Grid.Column<ConfigurationItem> nameColumn = grid.getColumnByKey("name");
        Grid.Column<ConfigurationItem> valueColumn = grid.getColumnByKey("value");

        com.vaadin.flow.component.grid.HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(nameColumn).setComponent(nameFilter);
        headerRow.getCell(valueColumn).setComponent(valueFilter);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(ConfigurationItem value) {
        this.configurationItem = value;
        binder.readBean(this.configurationItem);
    }
}
