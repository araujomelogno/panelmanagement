package uy.com.equipos.panelmanagement.views.panels;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.services.PanelService;
import uy.com.equipos.panelmanagement.services.PanelistService; // Added

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ViewPanelistsDialog extends Dialog {

    private Panel panel;
    private PanelService panelService;
    private PanelistService panelistService; // Added

    private Grid<Panelist> grid = new Grid<>(Panelist.class, false);
    private ListDataProvider<Panelist> dataProvider;
    private List<Panelist> panelistList = new ArrayList<>();

    public ViewPanelistsDialog(Panel panel, PanelService panelService, PanelistService panelistService) { // Added panelistService
        this.panel = panel;
        this.panelService = panelService;
        this.panelistService = panelistService; // Added

        setWidth("80%");
        setHeight("70%");

        configureGrid();
        loadPanelists();

        VerticalLayout layout = new VerticalLayout(grid);
        layout.setSizeFull();
        add(layout);

        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
    }

    private void configureGrid() {
        // Delete button column
        grid.addComponentColumn(panelist -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            deleteButton.addClickListener(event -> confirmRemovePanelist(panelist));
            return deleteButton;
        }).setHeader("").setFlexGrow(0).setWidth("80px").setKey("delete-column"); // Added key for completeness, though not used for filtering

        Grid.Column<Panelist> firstNameColumn = grid.addColumn(Panelist::getFirstName).setHeader("Nombre").setSortable(true).setAutoWidth(true).setKey("firstName");
        Grid.Column<Panelist> lastNameColumn = grid.addColumn(Panelist::getLastName).setHeader("Apellido").setSortable(true).setAutoWidth(true).setKey("lastName");
        Grid.Column<Panelist> emailColumn = grid.addColumn(Panelist::getEmail).setHeader("Email").setSortable(true).setAutoWidth(true).setKey("email");

        HeaderRow filterRow = grid.appendHeaderRow();

        TextField firstNameFilter = new TextField();
        firstNameFilter.setPlaceholder("Filtrar...");
        firstNameFilter.addValueChangeListener(event -> applyFilters());
        filterRow.getCell(firstNameColumn).setComponent(firstNameFilter);

        TextField lastNameFilter = new TextField();
        lastNameFilter.setPlaceholder("Filtrar...");
        lastNameFilter.addValueChangeListener(event -> applyFilters());
        filterRow.getCell(lastNameColumn).setComponent(lastNameFilter);

        TextField emailFilter = new TextField();
        emailFilter.setPlaceholder("Filtrar...");
        emailFilter.addValueChangeListener(event -> applyFilters());
        filterRow.getCell(emailColumn).setComponent(emailFilter);
    }

    private void confirmRemovePanelist(Panelist panelist) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar Eliminación");
        dialog.setText("¿Está seguro que desea eliminar al panelista '" + panelist.getFirstName() + " " + panelist.getLastName() + "' de este panel?");
        dialog.setConfirmText("Eliminar");
        dialog.setConfirmButtonTheme("error primary");
        dialog.setCancelText("Cancelar");
        dialog.addConfirmListener(event -> removePanelistFromPanel(panelist));
        dialog.open();
    }

    private void removePanelistFromPanel(Panelist panelist) {
        panelistService.removePanelFromPanelist(panelist.getId(), panel.getId());
        loadPanelists(); // Refresh grid
    }

    private void loadPanelists() {
        if (panel != null && panel.getId() != null) {
            panelService.getWithPanelists(panel.getId()).ifPresent(p -> {
                panelistList.clear();
                panelistList.addAll(p.getPanelists());
                dataProvider = new ListDataProvider<>(panelistList);
                grid.setDataProvider(dataProvider);
            });
        } else {
            panelistList.clear();
            grid.setItems(Collections.emptyList());
        }
        applyFilters(); // Apply filters after loading
    }

    private void applyFilters() {
        if (dataProvider == null) return;

        dataProvider.clearFilters(); // Clear previous filters

        HeaderRow filterHeader = grid.getHeaderRows().stream()
            .filter(hr -> hr.getCells().stream().anyMatch(cell -> cell.getComponent() instanceof TextField))
            .findFirst().orElse(null);

        if (filterHeader == null) return;

        String firstNameFilterValue = ((TextField) filterHeader.getCell(grid.getColumnByKey("firstName")).getComponent()).getValue().toLowerCase().trim();
        String lastNameFilterValue = ((TextField) filterHeader.getCell(grid.getColumnByKey("lastName")).getComponent()).getValue().toLowerCase().trim();
        String emailFilterValue = ((TextField) filterHeader.getCell(grid.getColumnByKey("email")).getComponent()).getValue().toLowerCase().trim();

        List<Panelist> filteredList = panelistList.stream()
                .filter(p -> p.getFirstName().toLowerCase().contains(firstNameFilterValue))
                .filter(p -> p.getLastName().toLowerCase().contains(lastNameFilterValue))
                .filter(p -> p.getEmail().toLowerCase().contains(emailFilterValue))
                .collect(Collectors.toList());

        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(filteredList);
        dataProvider.refreshAll();
    }
}
