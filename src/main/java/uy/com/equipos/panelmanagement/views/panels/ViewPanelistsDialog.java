package uy.com.equipos.panelmanagement.views.panels;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.services.PanelService;

import java.util.Collections;

public class ViewPanelistsDialog extends Dialog {

    private Panel panel;
    private PanelService panelService;

    private Grid<Panelist> grid = new Grid<>(Panelist.class, false);

    public ViewPanelistsDialog(Panel panel, PanelService panelService) {
        this.panel = panel;
        this.panelService = panelService;

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
        grid.addColumn(Panelist::getFirstName).setHeader("Nombre").setAutoWidth(true);
        grid.addColumn(Panelist::getLastName).setHeader("Apellido").setAutoWidth(true);
        grid.addColumn(Panelist::getEmail).setHeader("Email").setAutoWidth(true);
        // Add more columns as needed, e.g., phone, etc.
    }

    private void loadPanelists() {
        if (panel != null && panel.getId() != null) {
            panelService.getWithPanelists(panel.getId()).ifPresent(p -> {
                grid.setItems(p.getPanelists());
            });
        } else {
            grid.setItems(Collections.emptyList());
        }
    }
}
