package uy.com.equipos.panelmanagement.views.propierties;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;

@PageTitle("Propiedades de panelistas")
@Route("master-detail/:panelistPropertyID?/:action?(edit)")
@Menu(order = 2, icon = LineAwesomeIconUrl.EDIT)
@PermitAll
public class PropiertiesView extends Div implements BeforeEnterObserver {

    private final String PANELISTPROPERTY_ID = "panelistPropertyID";
    private final String PANELISTPROPERTY_EDIT_ROUTE_TEMPLATE = "master-detail/%s/edit";

    private final Grid<PanelistProperty> grid = new Grid<>(PanelistProperty.class, false);
    private Div editorLayoutDiv; // Declarado como miembro de la clase


    // Campos de filtro
    private TextField nameFilter = new TextField();
    private TextField typeFilter = new TextField();


    // Campos de filtro
    private TextField nameFilter = new TextField();
    private TextField typeFilter = new TextField();
 
    private TextField name;
    private TextField type;

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");

    private Button nuevaPropiedadButton;

    private final BeanValidationBinder<PanelistProperty> binder;

    private PanelistProperty panelistProperty;

    private final PanelistPropertyService panelistPropertyService;

    public PropiertiesView(PanelistPropertyService panelistPropertyService) {
        this.panelistPropertyService = panelistPropertyService;
        addClassNames("propierties-view");

        // Configurar columnas del Grid PRIMERO
        grid.addColumn(PanelistProperty::getName).setHeader("Nombre").setKey("name").setAutoWidth(true);
        grid.addColumn(PanelistProperty::getType).setHeader("Tipo").setKey("type").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // Create UI - SplitLayout
        SplitLayout splitLayout = new SplitLayout();
        // createGridLayout ahora puede acceder a las keys de las columnas de forma segura
        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);
        // editorLayoutDiv.setVisible(false); // Se maneja después de add(mainLayout)

        // Crear barra de título
        H2 pageTitleText = new H2("Propiedades de Panelistas");
        nuevaPropiedadButton = new Button("Nueva Propiedad");
        HorizontalLayout titleBar = new HorizontalLayout(pageTitleText, nuevaPropiedadButton);
        titleBar.setWidthFull();
        titleBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        titleBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout mainLayout = new VerticalLayout(titleBar, splitLayout);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);

        add(mainLayout);
        if (editorLayoutDiv != null) {
            editorLayoutDiv.setVisible(false);
        }
 
        // Listener para el botón "Nueva Propiedad"
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

        editorLayoutDiv.setVisible(false); // Ocultar el editor inicialmente
        add(splitLayout);


        // Configurar placeholders para filtros
        nameFilter.setPlaceholder("Filtrar por Nombre");
        typeFilter.setPlaceholder("Filtrar por Tipo");

        // Añadir listeners para refrescar el grid
        nameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        typeFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        // Configurar el DataProvider del Grid
        grid.setItems(query -> {
            String nameVal = nameFilter.getValue();
            String typeVal = typeFilter.getValue();

            return panelistPropertyService.list(
                VaadinSpringDataHelpers.toSpringPageRequest(query),
                nameVal,
                typeVal
            ).stream();
        });

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                editorLayoutDiv.setVisible(true);
                UI.getCurrent().navigate(String.format(PANELISTPROPERTY_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm(); // clearForm ahora también oculta el editor
                UI.getCurrent().navigate(PropiertiesView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(PanelistProperty.class);

        // Bind fields. This is where you'd define e.g. validation rules
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.panelistProperty == null) {
                    this.panelistProperty = new PanelistProperty();
                }
                binder.writeBean(this.panelistProperty);
                panelistPropertyService.save(this.panelistProperty);
                clearForm();
                refreshGrid();
                Notification.show("Datos actualizados");
                UI.getCurrent().navigate(PropiertiesView.class);
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
        Optional<Long> panelistPropertyId = event.getRouteParameters().get(PANELISTPROPERTY_ID).map(Long::parseLong);
        if (panelistPropertyId.isPresent()) {
            Optional<PanelistProperty> panelistPropertyFromBackend = panelistPropertyService
                    .get(panelistPropertyId.get());
            if (panelistPropertyFromBackend.isPresent()) {
                populateForm(panelistPropertyFromBackend.get());
                editorLayoutDiv.setVisible(true);
            } else {
                Notification.show(String.format("La propiedad de panelista solicitada no fue encontrada, ID = %s",
                        panelistPropertyId.get()), 3000, Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                if (editorLayoutDiv != null) {
                    editorLayoutDiv.setVisible(false);
                }
                event.forwardTo(PropiertiesView.class);
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
        type = new TextField("Tipo");
        formLayout.add(name, type);

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
        headerRow.getCell(grid.getColumnByKey("type")).setComponent(typeFilter);
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

    private void populateForm(PanelistProperty value) {
        this.panelistProperty = value;
        binder.readBean(this.panelistProperty);

    }
}
