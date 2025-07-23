package uy.com.equipos.panelmanagement.views;

import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import uy.com.equipos.panelmanagement.data.ConfigurationItem;
import uy.com.equipos.panelmanagement.services.ConfigurationItemService;

@PageTitle("Reclutamiento")
@Route(value = "recruitment", layout = MainLayout.class)
@PermitAll
public class RecruitmentView extends Div {

    private final ConfigurationItemService configurationItemService;

    private static final String RECRUITMENT_INTRODUCTION_NAME = "recruitment.introduction";
    private static final String RECRUITMENT_LANDING_URL_NAME = "recruitment.landing.url";
    private static final String RECRUITMENT_ALCHEMER_STUDY_ID_NAME = "recruitment.alchemer.study.id";
    private static final String RECRUITMENT_RETRY_NAME = "recruitment.retry";

    private TextArea introductionArea;
    private TextField landingUrlField;
    private TextField alchemerStudyIdField;
    private TextField retryField;
    private Button saveButton;

    public RecruitmentView(ConfigurationItemService configurationItemService) {
        this.configurationItemService = configurationItemService;
        addClassName("recruitment-view");

        introductionArea = new TextArea("Introducción");
        introductionArea.setWidthFull();
        introductionArea.setReadOnly(true);

        landingUrlField = new TextField("Url del Landing Page:");
        landingUrlField.setWidthFull();

        alchemerStudyIdField = new TextField("Id de estudio de Alchemer");
        alchemerStudyIdField.setWidthFull();
        alchemerStudyIdField.setPattern("[0-9]*");

        retryField = new TextField("Cantidad de reintentos de envío del consentimiento");
        retryField.setWidthFull();
        retryField.setPattern("[0-9]*");

        saveButton = new Button("Guardar");
        saveButton.addClickListener(e -> saveConfiguration());

        VerticalLayout layout = new VerticalLayout(introductionArea, landingUrlField, alchemerStudyIdField, retryField, saveButton);
        layout.setSizeFull();
        add(layout);

        loadConfiguration();
    }

    private void loadConfiguration() {
        Optional<ConfigurationItem> introduction = configurationItemService.getByName(RECRUITMENT_INTRODUCTION_NAME);
        introduction.ifPresent(item -> introductionArea.setValue(item.getValue()));

        Optional<ConfigurationItem> landingUrl = configurationItemService.getByName(RECRUITMENT_LANDING_URL_NAME);
        landingUrl.ifPresent(item -> landingUrlField.setValue(item.getValue()));

        Optional<ConfigurationItem> alchemerStudyId = configurationItemService.getByName(RECRUITMENT_ALCHEMER_STUDY_ID_NAME);
        alchemerStudyId.ifPresent(item -> alchemerStudyIdField.setValue(item.getValue()));

        Optional<ConfigurationItem> retry = configurationItemService.getByName(RECRUITMENT_RETRY_NAME);
        retry.ifPresent(item -> retryField.setValue(item.getValue()));
    }

    private void saveConfiguration() {
        // Introduction
        Optional<ConfigurationItem> optionalIntroduction = configurationItemService.getByName(RECRUITMENT_INTRODUCTION_NAME);
        ConfigurationItem introduction = optionalIntroduction.orElse(new ConfigurationItem());
        introduction.setName(RECRUITMENT_INTRODUCTION_NAME);
        introduction.setValue(introductionArea.getValue());
        configurationItemService.update(introduction);

        // Landing URL
        Optional<ConfigurationItem> optionalLandingUrl = configurationItemService.getByName(RECRUITMENT_LANDING_URL_NAME);
        ConfigurationItem landingUrl = optionalLandingUrl.orElse(new ConfigurationItem());
        landingUrl.setName(RECRUITMENT_LANDING_URL_NAME);
        landingUrl.setValue(landingUrlField.getValue());
        configurationItemService.update(landingUrl);

        // Alchemer Study ID
        Optional<ConfigurationItem> optionalAlchemerStudyId = configurationItemService.getByName(RECRUITMENT_ALCHEMER_STUDY_ID_NAME);
        ConfigurationItem alchemerStudyId = optionalAlchemerStudyId.orElse(new ConfigurationItem());
        alchemerStudyId.setName(RECRUITMENT_ALCHEMER_STUDY_ID_NAME);
        alchemerStudyId.setValue(alchemerStudyIdField.getValue());
        configurationItemService.update(alchemerStudyId);

        // Retry
        Optional<ConfigurationItem> optionalRetry = configurationItemService.getByName(RECRUITMENT_RETRY_NAME);
        ConfigurationItem retry = optionalRetry.orElse(new ConfigurationItem());
        retry.setName(RECRUITMENT_RETRY_NAME);
        retry.setValue(retryField.getValue());
        configurationItemService.update(retry);

        Notification.show("Configuración de reclutamiento guardada.");
    }
}
