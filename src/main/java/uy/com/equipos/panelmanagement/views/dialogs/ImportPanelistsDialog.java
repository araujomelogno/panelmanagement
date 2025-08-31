package uy.com.equipos.panelmanagement.views.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import uy.com.equipos.panelmanagement.services.PanelistPropertyService;
import uy.com.equipos.panelmanagement.services.PanelistService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import uy.com.equipos.panelmanagement.data.PanelistProperty;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportPanelistsDialog extends Dialog {

    private record ColumnMapping(String excelHeader, String panelistAttribute) {}

    private static final Logger logger = LoggerFactory.getLogger(ImportPanelistsDialog.class);

    private final PanelistService panelistService;
    private final PanelistPropertyService panelistPropertyService;
    private final Runnable onSuccessCallback;

    private VerticalLayout contentLayout = new VerticalLayout();
    private MemoryBuffer buffer = new MemoryBuffer();
    private Upload upload = new Upload(buffer);
    private byte[] fileContent;
    private Button nextButton = new Button("Siguiente");
    private Button closeButton = new Button("Cerrar");

    public ImportPanelistsDialog(PanelistService panelistService, PanelistPropertyService panelistPropertyService, Runnable onSuccessCallback) {
        this.panelistService = panelistService;
        this.panelistPropertyService = panelistPropertyService;
        this.onSuccessCallback = onSuccessCallback;

        setHeaderTitle("Importar Panelistas (Paso 1 de 2)");

        setupUpload();
        setupButtons();

        contentLayout.add(upload);
        add(contentLayout);

        getFooter().add(closeButton, nextButton);
    }

    private void setupUpload() {
        upload.setAcceptedFileTypes(".xlsx", ".xls");
        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> {
            try (InputStream inputStream = buffer.getInputStream()) {
                fileContent = inputStream.readAllBytes();
                logger.info("Archivo {} subido exitosamente y guardado en memoria.", event.getFileName());
                nextButton.setEnabled(true);
            } catch (java.io.IOException e) {
                logger.error("Error al leer el archivo subido", e);
                Notification.show("Error al leer el archivo: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }

    private void setupButtons() {
        nextButton.setEnabled(false);
        nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        nextButton.addClickListener(e -> moveToStep2());

        closeButton.addClickListener(e -> this.close());
    }

    private void moveToStep2() {
        if (fileContent == null || fileContent.length == 0) {
            Notification.show("No se ha subido ningún archivo o el archivo está vacío.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try (InputStream inputStream = new java.io.ByteArrayInputStream(fileContent)) {
            logger.info("Procesando archivo para pasar al paso 2.");
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                Notification.show("El archivo Excel no tiene una fila de encabezado.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }

            logger.info("Encabezados encontrados: {}", headers);
            showMappingScreen(headers);

        } catch (Exception ex) {
            logger.error("Error al procesar el archivo Excel", ex);
            Notification.show("Error al leer el archivo Excel: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showMappingScreen(List<String> headers) {
        setHeaderTitle("Importar Panelistas (Paso 2 de 2)");
        contentLayout.removeAll();

        Grid<ColumnMapping> grid = new Grid<>();
        List<ColumnMapping> mappings = headers.stream().map(header -> new ColumnMapping(header, null)).collect(Collectors.toList());
        grid.setItems(mappings);

        List<String> panelistAttributes = Stream.concat(
                Stream.of("Nombre", "Apellido", "E-mail", "Celular", "Fuente", "Id_En_Fuente"),
                panelistPropertyService.findAll().stream().map(PanelistProperty::getName)
        ).collect(Collectors.toList());

        grid.addColumn(ColumnMapping::excelHeader).setHeader("Columna en Excel");

        Map<String, ComboBox<String>> comboBoxes = new HashMap<>();
        grid.addComponentColumn(mapping -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setItems(panelistAttributes);
            comboBox.setPlaceholder("Seleccionar atributo...");
            comboBoxes.put(mapping.excelHeader(), comboBox);
            return comboBox;
        }).setHeader("Atributo del Panelista");

        contentLayout.add(grid);

        Button backButton = new Button("Atrás", e -> showUploadScreen());
        Button importButton = new Button("Importar", e -> {
            Map<String, String> finalMappings = new HashMap<>();
            for (Map.Entry<String, ComboBox<String>> entry : comboBoxes.entrySet()) {
                if (entry.getValue().getValue() != null) {
                    finalMappings.put(entry.getKey(), entry.getValue().getValue());
                }
            }
            importPanelists(finalMappings);
        });
        importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().removeAll();
        getFooter().add(backButton, importButton);
    }

    private void importPanelists(Map<String, String> mappings) {
        if (fileContent == null || fileContent.length == 0) {
            Notification.show("No se encontró contenido de archivo para importar.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            int createdCount = panelistService.importPanelistsFromData(mappings, fileContent);
            logger.info("Se han guardado {} panelistas.", createdCount);
            Notification.show("Panelistas importados exitosamente: " + createdCount, 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            this.close();
        } catch (Exception ex) {
            logger.error("Error durante la importación de panelistas", ex);
            Notification.show("Error durante la importación: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showUploadScreen() {
        setHeaderTitle("Importar Panelistas (Paso 1 de 2)");
        this.fileContent = null;
        nextButton.setEnabled(false);
        contentLayout.removeAll();
        contentLayout.add(upload);
        getFooter().removeAll();
        getFooter().add(closeButton, nextButton);
    }
}
