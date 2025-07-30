package uy.com.equipos.panelmanagement.views.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import uy.com.equipos.panelmanagement.services.dtos.AlchemerContactDto;

import java.util.List;

public class AlchemerSubmissionsDialog extends Dialog {

    public AlchemerSubmissionsDialog(List<AlchemerContactDto> contacts) {
        setHeaderTitle("Envíos de Alchemer");
        setWidth("80%");
        setHeight("70%");

        // Create text fields for contact counts
        TextField totalContactsField = new TextField("Contactos");
        totalContactsField.setValue(String.valueOf(contacts.size()));
        totalContactsField.setReadOnly(true);

        long clickedContacts = contacts.stream().filter(c -> "Click".equals(c.getSendStatus())).count();
        TextField clickedContactsField = new TextField("Contactos que hicieron click");
        clickedContactsField.setValue(String.valueOf(clickedContacts));
        clickedContactsField.setReadOnly(true);

        HorizontalLayout countsLayout = new HorizontalLayout(totalContactsField, clickedContactsField);
        countsLayout.setSpacing(true);

        // Create grid
        Grid<AlchemerContactDto> grid = new Grid<>(AlchemerContactDto.class, false);
        grid.addColumn(AlchemerContactDto::getEmailAddress).setHeader("Email");
        grid.addColumn(AlchemerContactDto::getStatus).setHeader("Status");
        grid.addColumn(AlchemerContactDto::getSubscriberStatus).setHeader("Subscriber Status");
        grid.addColumn(AlchemerContactDto::getSendStatus).setHeader("Send Status");
        grid.addColumn(AlchemerContactDto::getResponseStatus).setHeader("Response Status");
        grid.setItems(contacts);

        // Layout for the dialog content
        VerticalLayout dialogLayout = new VerticalLayout(countsLayout, grid);
        dialogLayout.setSizeFull();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        add(dialogLayout);

        // Close button
        Button closeButton = new Button("Cerrar", e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getFooter().add(closeButton);
    }
}
