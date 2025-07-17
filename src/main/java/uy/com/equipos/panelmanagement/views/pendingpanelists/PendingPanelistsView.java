package uy.com.equipos.panelmanagement.views.pendingpanelists;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

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
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.Status;
import uy.com.equipos.panelmanagement.services.PanelistService;

@PageTitle("Panelistas Pendientes")
@Route("pending-panelists/:panelistID?/:action?(edit)")
@Menu(order = 5, icon = LineAwesomeIconUrl.BELL)
@PermitAll
public class PendingPanelistsView extends Div implements BeforeEnterObserver {

    private final String PANELIST_ID = "panelistID";
    private final String PANELIST_EDIT_ROUTE_TEMPLATE = "pending-panelists/%s/edit";

    private final Grid<Panelist> grid = new Grid<>(Panelist.class, false);
    private Div editorLayoutDiv;

    // Form fields
    private TextField firstName;
    private TextField lastName;
    private TextField email;
    private TextField phone;

    // Filter fields
    private TextField firstNameFilter = new TextField();
    private TextField lastNameFilter = new TextField();
    private TextField emailFilter = new TextField();
    private TextField phoneFilter = new TextField();

    private final Button cancel = new Button("Cancelar");
    private final Button save = new Button("Guardar");
    private Button deleteButton;
    private Button newPanelistButton;

    private final BeanValidationBinder<Panelist> binder;

    private Panelist panelist;

    private final PanelistService panelistService;

    public PendingPanelistsView(PanelistService panelistService) {
        this.panelistService = panelistService;
        addClassNames("pending-panelists-view");

        deleteButton = new Button("Eliminar");
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        deleteButton.addClickListener(e -> onDeleteClicked());

        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        newPanelistButton = new Button("Nuevo Panelista");
        newPanelistButton.getStyle().set("margin-left", "18px");
        VerticalLayout mainLayout = new VerticalLayout(newPanelistButton, splitLayout);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);

        add(mainLayout);
        if (this.editorLayoutDiv != null) {
            this.editorLayoutDiv.setVisible(false);
        }

        newPanelistButton.addClickListener(click -> {
            grid.asSingleSelect().clear();
            populateForm(new Panelist());
            if (editorLayoutDiv != null) {
                editorLayoutDiv.setVisible(true);
            }
            if (firstName != null) {
                firstName.focus();
            }
        });

        Grid.Column<Panelist> firstNameCol = grid.addColumn(Panelist::getFirstName).setHeader("Nombre").setKey("firstName").setAutoWidth(true);
        Grid.Column<Panelist> lastNameCol = grid.addColumn(Panelist::getLastName).setHeader("Apellido").setKey("lastName").setAutoWidth(true);
        Grid.Column<Panelist> emailCol = grid.addColumn(Panelist::getEmail).setHeader("Correo Electrónico").setKey("email").setAutoWidth(true);
        Grid.Column<Panelist> phoneCol = grid.addColumn(Panelist::getPhone).setHeader("Teléfono").setKey("phone").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        HeaderRow filterRow = grid.appendHeaderRow();

        firstNameFilter.setPlaceholder("Filtrar por Nombre...");
        filterRow.getCell(firstNameCol).setComponent(firstNameFilter);

        lastNameFilter.setPlaceholder("Filtrar por Apellido...");
        filterRow.getCell(lastNameCol).setComponent(lastNameFilter);

        emailFilter.setPlaceholder("Filtrar por Email...");
        filterRow.getCell(emailCol).setComponent(emailFilter);

        phoneFilter.setPlaceholder("Filtrar por Teléfono...");
        filterRow.getCell(phoneCol).setComponent(phoneFilter);

        firstNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        lastNameFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        emailFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());
        phoneFilter.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        grid.setItems(query -> {
            Pageable pageable = VaadinSpringDataHelpers.toSpringPageRequest(query);
            String fName = firstNameFilter.getValue();
            String lName = lastNameFilter.getValue();
            String mail = emailFilter.getValue();
            String ph = phoneFilter.getValue();
            return panelistService.listByStatus(pageable, fName, lName, mail, ph, Status.PENDIENTE).stream();
        });

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(PANELIST_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(PendingPanelistsView.class);
            }
        });

        binder = new BeanValidationBinder<>(Panelist.class);
        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.panelist == null) {
                    this.panelist = new Panelist();
                }
                binder.writeBean(this.panelist);
                panelistService.save(this.panelist);
                clearForm();
                refreshGrid();
                Notification.show("Datos actualizados");
                UI.getCurrent().navigate(PendingPanelistsView.class);
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
        Optional<Long> panelistId = event.getRouteParameters().get(PANELIST_ID).map(Long::parseLong);
        if (panelistId.isPresent()) {
            Optional<Panelist> panelistFromBackend = panelistService.get(panelistId.get());
            if (panelistFromBackend.isPresent()) {
                populateForm(panelistFromBackend.get());
                if (this.editorLayoutDiv != null) {
                    this.editorLayoutDiv.setVisible(true);
                }
            } else {
                Notification.show(String.format("El panelista no fue encontrado, ID = %s", panelistId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                refreshGrid();
                if (this.editorLayoutDiv != null) {
                    this.editorLayoutDiv.setVisible(false);
                }
                event.forwardTo(PendingPanelistsView.class);
            }
        } else {
            clearForm();
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        this.editorLayoutDiv = new Div();
        this.editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        this.editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        firstName = new TextField("Nombre");
        lastName = new TextField("Apellido");
        email = new TextField("Correo Electrónico");
        phone = new TextField("Teléfono");
        formLayout.add(firstName, lastName, email, phone);

        editorDiv.add(formLayout);
        createButtonLayout(this.editorLayoutDiv);

        splitLayout.addToSecondary(this.editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, deleteButton, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void populateForm(Panelist value) {
        this.panelist = value;
        binder.readBean(this.panelist);

        if (deleteButton != null) {
            deleteButton.setEnabled(value != null && value.getId() != null);
        }
    }

    private void clearForm() {
        populateForm(null);
        if (this.editorLayoutDiv != null) {
            this.editorLayoutDiv.setVisible(false);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(false);
        }
    }

    private void onDeleteClicked() {
        if (this.panelist == null || this.panelist.getId() == null) {
            Notification.show("No hay panelista seleccionado para eliminar.", 3000, Notification.Position.MIDDLE);
            return;
        }

        com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
        dialog.setHeader("Confirmar Eliminación");
        dialog.setText("¿Está seguro de que desea eliminar al panelista '" + this.panelist.getFirstName() + " "
                + this.panelist.getLastName() + "'?");

        dialog.setConfirmText("Eliminar");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelText("Cancelar");

        dialog.addConfirmListener(event -> {
            try {
                panelistService.delete(this.panelist.getId());
                clearForm();
                refreshGrid();
                Notification.show("Panelista eliminado correctamente.", 3000, Notification.Position.BOTTOM_START);
                UI.getCurrent().navigate(PendingPanelistsView.class);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                Notification.show(
                        "No se puede eliminar el panelista. Es posible que esté siendo referenciado por otras entidades.",
                        5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                Notification.show("Ocurrió un error al intentar eliminar el panelista: " + ex.getMessage(), 5000,
                        Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }
}
