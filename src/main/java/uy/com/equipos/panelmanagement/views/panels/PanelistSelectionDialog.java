package uy.com.equipos.panelmanagement.views.panels;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span; // Added
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment; // Added
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.services.PanelService; // Added
import uy.com.equipos.panelmanagement.services.PanelistService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional; // Added for orElseThrow and orElse
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PanelistSelectionDialog extends Dialog {

    private final PanelService panelServiceForPanel; // Service for Panel entities
    private final PanelistService panelistServiceForPanelist; // Service for Panelist entities
    private final Panel currentPanel;
    private final Map<PanelistProperty, Object> filterCriteria;
    private final PanelistPropertyFilterDialog ownerDialog; // Added

    private VerticalLayout contentLayout; // Made field
    private Grid<Panelist> panelistsGrid;
    private List<Panelist> availablePanelists;
    private Set<Panelist> selectedPanelists = new HashSet<>();

    private Button selectAllButton = new Button("Seleccionar Todos");
    private Button deselectAllButton = new Button("Deseleccionar Todos"); // Added for better UX
    private Button cancelButton = new Button("Cancelar");
    private Button saveButton = new Button("Guardar");

    public PanelistSelectionDialog(
            PanelService panelServiceForPanel,
            PanelistService panelistServiceForPanelist,
            Panel currentPanel,
            Map<PanelistProperty, Object> filterCriteria,
            PanelistPropertyFilterDialog ownerDialog) { // Added ownerDialog
        this.panelServiceForPanel = panelServiceForPanel;
        this.panelistServiceForPanelist = panelistServiceForPanelist;
        this.currentPanel = currentPanel;
        this.filterCriteria = filterCriteria != null ? filterCriteria : new HashMap<>();
        this.ownerDialog = ownerDialog; // Added

        setHeaderTitle("Buscar Panelistas - Paso 2: Seleccionar Panelistas");
        setWidth("80%"); // Make dialog wider
        setHeight("70%");

        contentLayout = new VerticalLayout(); // Initialize field
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        contentLayout.setSizeFull();
        contentLayout.setAlignItems(Alignment.STRETCH);


        panelistsGrid = new Grid<>(Panelist.class, false);
        panelistsGrid.setSelectionMode(Grid.SelectionMode.NONE); // We handle selection with checkboxes

        panelistsGrid.addComponentColumn(panelist -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(selectedPanelists.contains(panelist));
            checkbox.addValueChangeListener(event -> {
                if (event.getValue()) {
                    selectedPanelists.add(panelist);
                } else {
                    selectedPanelists.remove(panelist);
                }
                updateButtonStates();
            });
            return checkbox;
        }).setHeader("Seleccionar").setFlexGrow(0).setWidth("120px");

        panelistsGrid.addColumn(Panelist::getFirstName).setHeader("Nombre").setSortable(true);
        panelistsGrid.addColumn(Panelist::getLastName).setHeader("Apellido").setSortable(true);
        panelistsGrid.addColumn(Panelist::getEmail).setHeader("Email").setSortable(true);
        // TODO: Add other relevant Panelist fields as columns if needed

        // Fetch panelists based on the provided criteria
        if (this.panelistServiceForPanelist != null) {
            this.availablePanelists = panelistServiceForPanelist.findPanelistsByCriteria(this.filterCriteria);
        } else {
            System.err.println("PanelistService (for Panelist) is null in PanelistSelectionDialog. Cannot fetch panelists.");
            this.availablePanelists = new ArrayList<>();
        }

        if (this.availablePanelists == null || this.availablePanelists.isEmpty()) {
            panelistsGrid.setVisible(false);
            Span noPanelistsMessage = new Span("No se encontraron panelistas con los criterios especificados.");
            noPanelistsMessage.getStyle().set("text-align", "center").set("font-style", "italic");
            contentLayout.add(noPanelistsMessage);
            selectAllButton.setEnabled(false);
            deselectAllButton.setEnabled(false);
            // saveButton is handled by updateButtonStates based on selection
        } else {
            panelistsGrid.setItems(this.availablePanelists);
            panelistsGrid.setSizeFull();
            contentLayout.add(panelistsGrid);
            contentLayout.setFlexGrow(1, panelistsGrid); // Make grid take available space
        }

        add(contentLayout);

        setupButtonLogic();
        getFooter().add(selectAllButton, deselectAllButton, saveButton, cancelButton);
        updateButtonStates();
    }

    private void setupButtonLogic() {
        selectAllButton.addClickListener(e -> {
            selectedPanelists.addAll(availablePanelists);
            panelistsGrid.getDataProvider().refreshAll();
            updateButtonStates();
        });

        deselectAllButton.addClickListener(e -> {
            selectedPanelists.clear();
            panelistsGrid.getDataProvider().refreshAll();
            updateButtonStates();
        });

        cancelButton.addClickListener(e -> close());

        saveButton.addClickListener(e -> {
            if (this.currentPanel == null || this.currentPanel.getId() == null) { // Check ID for persisted panel
                Notification.show("Error: No se ha especificado un panel válido.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (this.selectedPanelists == null || this.selectedPanelists.isEmpty()) {
                Notification.show("No se han seleccionado panelistas.", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                Panel managedPanel = this.panelServiceForPanel.get(this.currentPanel.getId())
                    .orElseThrow(() -> {
                        Notification.show("Error: El panel ya no existe.", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return new IllegalStateException("Panel no longer exists: " + this.currentPanel.getId());
                    });

                Set<Panelist> panelistsToUpdate = new HashSet<>();
                int addedCount = 0;

                for (Panelist detachedSelectedPanelist : this.selectedPanelists) {
                    Panelist managedSelectedPanelist = this.panelistServiceForPanelist.get(detachedSelectedPanelist.getId())
                        .orElse(null);

                    if (managedSelectedPanelist == null) {
                        Notification.show("Advertencia: El panelista con ID " + detachedSelectedPanelist.getId() + " ya no existe y no se puede vincular.", 5000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_WARNING);
                        continue;
                    }

                    if (managedSelectedPanelist.getPanels() == null) {
                        managedSelectedPanelist.setPanels(new HashSet<>());
                    }
                    if (managedPanel.getPanelists() == null) {
                        managedPanel.setPanelists(new HashSet<>());
                    }

                    if (managedSelectedPanelist.getPanels().add(managedPanel)) {
                        managedPanel.getPanelists().add(managedSelectedPanelist);

                        panelistsToUpdate.add(managedSelectedPanelist);
                        addedCount++;
                    }
                }

                if (addedCount > 0) {
                    for (Panelist p : panelistsToUpdate) {
                        this.panelistServiceForPanelist.save(p);
                    }
                    Notification.show(addedCount + " panelista(s) vinculado(s) correctamente al panel: " + managedPanel.getName(), 5000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show("Los panelistas seleccionados ya estaban vinculados o no se pudieron vincular.", 3000, Notification.Position.MIDDLE);
                }

                if (this.ownerDialog != null) {
                    this.ownerDialog.closeDialog(); // Close the owner dialog (Step 1)
                }
                close(); // Close this dialog (Step 2)

            } catch (Exception ex) {
                Notification.show("Error al vincular panelistas: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace(); // For debugging
            }
        });
    }

    private void updateButtonStates() {
        boolean hasSelection = !selectedPanelists.isEmpty();
        boolean allSelected = !availablePanelists.isEmpty() && selectedPanelists.size() == availablePanelists.size();

        saveButton.setEnabled(hasSelection);
        selectAllButton.setEnabled(!allSelected);
        deselectAllButton.setEnabled(hasSelection);
    }
}
