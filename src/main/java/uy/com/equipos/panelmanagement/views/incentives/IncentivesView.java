package uy.com.equipos.panelmanagement.views.incentives;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import uy.com.equipos.panelmanagement.data.Incentive;
import uy.com.equipos.panelmanagement.services.IncentiveService;

@PageTitle("Incentivos")
@Route("incentives/:incentiveID?/:action?(edit)")
@Menu(order = 4, icon = LineAwesomeIconUrl.COLUMNS_SOLID)
@AnonymousAllowed
public class IncentivesView extends Div implements BeforeEnterObserver {

    private final String INCENTIVE_ID = "incentiveID";
    private final String INCENTIVE_EDIT_ROUTE_TEMPLATE = "incentives/%s/edit";

    private final Grid<Incentive> grid = new Grid<>(Incentive.class, false);
    private Div editorLayoutDiv; // Declarado como miembro de la clase

    // Campos de filtro
    private TextField nameFilter = new TextField();
    private TextField quantityAvailableFilter = new TextField();

    private TextField name;
    private TextField quantityAvailable;

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");

    private final BeanValidationBinder<Incentive> binder;

    private Incentive incentive;

    private final IncentiveService incentiveService;

    public IncentivesView(IncentiveService incentiveService) {
        this.incentiveService = incentiveService;
        addClassNames("incentives-view");

        // Configurar columnas del Grid PRIMERO
        grid.addColumn(Incentive::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
        grid.addColumn(Incentive::getQuantityAvailable).setHeader("Cantidad Disponible").setKey("quantityAvailable").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // Create UI - SplitLayout
        SplitLayout splitLayout = new SplitLayout();
        // createGridLayout ahora puede acceder a las keys de las columnas de forma segura
        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);
        editorLayoutDiv.setVisible(false); // Ocultar el editor inicialmente
        add(splitLayout);

        // Configurar placeholders para filtros
        nameFilter.setPlaceholder("Filtrar por Nombre");
        quantityAvailableFilter.setPlaceholder("Filtrar por Cantidad");

        // Añadir listeners para refrescar el grid
        nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        quantityAvailableFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        // Configurar el DataProvider del Grid
        grid.setItems(query -> {
            String nameVal = nameFilter.getValue();
            String quantityAvailableStrVal = quantityAvailableFilter.getValue();
            // La conversión a Integer se manejará en el servicio o al crear la Specification
            return incentiveService.list(
                VaadinSpringDataHelpers.toSpringPageRequest(query),
                nameVal,
                quantityAvailableStrVal
            ).stream();
        });

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editorLayoutDiv.setVisible(true);
                UI.getCurrent().navigate(String.format(INCENTIVE_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm(); // clearForm ahora también oculta el editor
                UI.getCurrent().navigate(IncentivesView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(Incentive.class);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.forField(quantityAvailable).withConverter(new StringToIntegerConverter("Solo se permiten números"))
                .bind("quantityAvailable");

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.incentive == null) {
                    this.incentive = new Incentive();
                }
                binder.writeBean(this.incentive);
                incentiveService.save(this.incentive);
                clearForm();
                refreshGrid();
                Notification.show("Datos actualizados");
                UI.getCurrent().navigate(IncentivesView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error al actualizar los datos. Otro usuario modificó el registro mientras usted realizaba cambios.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Fallo al actualizar los datos. Verifique nuevamente que todos los valores sean válidos");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> incentiveId = event.getRouteParameters().get(INCENTIVE_ID).map(Long::parseLong);
        if (incentiveId.isPresent()) {
            Optional<Incentive> incentiveFromBackend = incentiveService.get(incentiveId.get());
            if (incentiveFromBackend.isPresent()) {
                populateForm(incentiveFromBackend.get());
                editorLayoutDiv.setVisible(true);
            } else {
                Notification.show(String.format("El incentivo solicitado no fue encontrado, ID = %s", incentiveId.get()),
                        3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                if (editorLayoutDiv != null) {
                    editorLayoutDiv.setVisible(false);
                }
                event.forwardTo(IncentivesView.class);
            }
        } else {
            clearForm(); // Asegurar que el editor esté oculto si no hay ID
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        editorLayoutDiv = new Div(); // Instanciar el miembro de la clase
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        name = new TextField("Nombre");
        quantityAvailable = new TextField("Cantidad Disponible");
        formLayout.add(name, quantityAvailable);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);

        HeaderRow headerRow = grid.appendHeaderRow();
        headerRow.getCell(grid.getColumnByKey("name")).setComponent(nameFilter);
        headerRow.getCell(grid.getColumnByKey("quantityAvailable")).setComponent(quantityAvailableFilter);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
        if (editorLayoutDiv != null) { // Buena práctica verificar nulidad
            editorLayoutDiv.setVisible(false);
        }
    }

    private void populateForm(Incentive value) {
        this.incentive = value;
        binder.readBean(this.incentive);

    }
}
