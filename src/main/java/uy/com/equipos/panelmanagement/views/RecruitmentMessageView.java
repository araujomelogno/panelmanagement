package uy.com.equipos.panelmanagement.views;

import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import uy.com.equipos.panelmanagement.data.ConfigurationItem;
import uy.com.equipos.panelmanagement.services.ConfigurationItemService;

@PageTitle("Reclutamiento")
@Route(value = "recruitment", layout = MainLayout.class)
@PermitAll
public class RecruitmentMessageView extends Div {

    private final ConfigurationItemService configurationItemService;

    private static final String RECRUITMENT_MESSAGE_NAME = "recruitment.message.mail";

    private TextArea messageArea;
    private Button saveButton;

    public RecruitmentMessageView(ConfigurationItemService configurationItemService) {
        this.configurationItemService = configurationItemService;
        addClassName("recruitment-message-view");

        messageArea = new TextArea();
        messageArea.setWidthFull();
        messageArea.setHeight("400px"); // Adjust height as needed

        saveButton = new Button("Guardar");
        saveButton.addClickListener(e -> saveRecruitmentMessage());

        VerticalLayout layout = new VerticalLayout(messageArea, saveButton);
        layout.setSizeFull();
        add(layout);

        loadRecruitmentMessage();
    }

    private void loadRecruitmentMessage() {
        Optional<ConfigurationItem> recruitmentMessage = configurationItemService.getByName(RECRUITMENT_MESSAGE_NAME);
        recruitmentMessage.ifPresent(item -> messageArea.setValue(item.getValue()));
    }

    private void saveRecruitmentMessage() {
        Optional<ConfigurationItem> optionalRecruitmentMessage = configurationItemService.getByName(RECRUITMENT_MESSAGE_NAME);
        ConfigurationItem recruitmentMessage = optionalRecruitmentMessage.orElse(new ConfigurationItem());
        recruitmentMessage.setName(RECRUITMENT_MESSAGE_NAME);
        recruitmentMessage.setValue(messageArea.getValue());
        configurationItemService.update(recruitmentMessage);
        Notification.show("Mensaje de reclutamiento guardado.");
    }
}
