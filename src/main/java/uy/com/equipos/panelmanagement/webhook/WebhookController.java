package uy.com.equipos.panelmanagement.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistRepository;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipationRepository;
import uy.com.equipos.panelmanagement.data.SurveyRepository;
import uy.com.equipos.panelmanagement.webhook.dto.WebhookPayloadDto;

import java.time.LocalDate; // Changed from LocalDateTime to LocalDate
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final SurveyRepository surveyRepository;
    private final PanelistRepository panelistRepository;
    private final SurveyPanelistParticipationRepository surveyPanelistParticipationRepository;

    public WebhookController(SurveyRepository surveyRepository,
                             PanelistRepository panelistRepository,
                             SurveyPanelistParticipationRepository surveyPanelistParticipationRepository) {
        this.surveyRepository = surveyRepository;
        this.panelistRepository = panelistRepository;
        this.surveyPanelistParticipationRepository = surveyPanelistParticipationRepository;
    }

    @PostMapping("/survey-response")
    @Transactional // Important for database operations
    public ResponseEntity<String> handleSurveyResponse(@RequestBody WebhookPayloadDto payload) {
        if (payload == null || payload.getData() == null || payload.getData().getContact() == null) {
            logger.warn("Received incomplete payload");
            return ResponseEntity.badRequest().body("Incomplete payload: 'data' or 'contact' field is missing.");
        }

        // Extract survey_id and convert to String for alchemerSurveyId
        String alchemerSurveyId = String.valueOf(payload.getData().getSurveyId());
        String email = payload.getData().getContact().getEmail();

        if (email == null || email.trim().isEmpty()) {
            logger.warn("Email is missing in payload for alchemer_survey_id: {}", alchemerSurveyId);
            return ResponseEntity.badRequest().body("Email is missing in contact data.");
        }

        logger.info("Processing webhook for alchemer_survey_id: {} and email: {}", alchemerSurveyId, email);

        Optional<Survey> surveyOpt = surveyRepository.findByAlchemerSurveyId(alchemerSurveyId);
        if (!surveyOpt.isPresent()) {
            logger.warn("Survey not found with alchemer_survey_id: {}", alchemerSurveyId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Survey not found for alchemer_survey_id: " + alchemerSurveyId);
        }
        Survey survey = surveyOpt.get();

        Optional<Panelist> panelistOpt = panelistRepository.findByEmail(email);
        if (!panelistOpt.isPresent()) {
            logger.warn("Panelist not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Panelist not found for email: " + email);
        }
        Panelist panelist = panelistOpt.get();

        Optional<SurveyPanelistParticipation> participationOpt =
                surveyPanelistParticipationRepository.findBySurveyAndPanelist(survey, panelist);

        if (!participationOpt.isPresent()) {
            logger.warn("SurveyPanelistParticipation not found for survey_id: {} (internal id: {}) and panelist_id: {} (email: {})",
                        alchemerSurveyId, survey.getId(), panelist.getId(), email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Participation record not found for the given survey and panelist.");
        }
        SurveyPanelistParticipation participation = participationOpt.get();

        if (participation.isCompleted()) {
            logger.info("Participation for survey_id: {} and panelist_id: {} is already marked as completed on {}. No update needed.",
                        survey.getId(), panelist.getId(), participation.getDateCompleted());
            return ResponseEntity.ok("Participation already marked as completed. No update performed.");
        }

        participation.setCompleted(true);
        participation.setDateCompleted(LocalDate.now()); // Using LocalDate as per entity definition

        surveyPanelistParticipationRepository.save(participation);

        logger.info("Successfully updated participation. Survey internal_id: {}, Panelist internal_id: {}. Marked as completed on {}",
                    survey.getId(), panelist.getId(), participation.getDateCompleted());
        return ResponseEntity.ok("Webhook processed successfully. Participation updated.");
    }
}
