package uy.com.equipos.panelmanagement.views.dialogs;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.button.Button;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.services.PanelistService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PanelistResultsDialog extends Dialog {

    private final PanelistService panelistService;
    private final Map<PanelistProperty, Object> filterCriteria;

    private Grid<Panelist> grid;

    public PanelistResultsDialog(PanelistService panelistService, Map<PanelistProperty, Object> filterCriteria) {
        this.panelistService = panelistService;
        this.filterCriteria = filterCriteria;

        setHeaderTitle("Panelistas Filtrados");
        setWidth("80vw"); // 80% del viewport width
        setHeight("70vh"); // 70% del viewport height

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(true);

        configureGrid();
        loadPanelists();

        layout.add(grid);
        add(layout);

        Button closeButton = new Button("Cerrar", e -> close());
        getFooter().add(closeButton);
    }

    private void configureGrid() {
        grid = new Grid<>(Panelist.class, false);
        grid.addColumn(Panelist::getFirstName).setHeader("Nombre").setAutoWidth(true);
        grid.addColumn(Panelist::getLastName).setHeader("Apellido").setAutoWidth(true);
        grid.addColumn(Panelist::getEmail).setHeader("Correo Electrónico").setAutoWidth(true);
        grid.addColumn(Panelist::getPhone).setHeader("Teléfono").setAutoWidth(true);
        grid.addColumn(Panelist::getLastContacted).setHeader("Último Contacto").setAutoWidth(true);
        grid.addColumn(Panelist::getLastInterviewCompleted).setHeader("Última Entrevista").setAutoWidth(true);

        // Si se quieren mostrar las propiedades que se usaron para filtrar, se puede hacer aquí.
        // Por ejemplo, añadir columnas dinámicamente basadas en filterCriteria.keySet()
        // Esto puede ser complejo si los tipos de datos varían mucho.
        // Por ahora, mostramos información básica del panelista.

        grid.setSizeFull();
    }

    private void loadPanelists() {
        // Aquí llamamos al servicio para obtener los panelistas.
        // El método findPanelistsByCriteria será implementado/ajustado en el siguiente paso del plan.
        List<Panelist> panelists = panelistService.findPanelistsByCriteria(filterCriteria);
        grid.setItems(panelists);

        if (panelists.isEmpty()) {
            // Opcional: Mostrar un mensaje si no hay resultados
            // Por ejemplo, añadiendo un Span al layout o usando un EmptyState de Vaadin si existe.
        }
    }
}
