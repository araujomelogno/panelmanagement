package uy.com.equipos.panelmanagement.webhook;

import java.time.LocalDate; // Changed from LocalDateTime to LocalDate
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uy.com.equipos.panelmanagement.data.ConfigurationItem;
import uy.com.equipos.panelmanagement.data.ConfigurationItemRepository;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistRepository;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipationRepository;
import uy.com.equipos.panelmanagement.data.SurveyRepository;
import uy.com.equipos.panelmanagement.data.Task;
import uy.com.equipos.panelmanagement.data.TaskRepository;
import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.TaskStatus;
import uy.com.equipos.panelmanagement.webhook.dto.AlchemerSurveyCompletionPayloadDto;

@RestController
@RequestMapping("/api/webhook")
public class AlchemerSurveyCompletionController {

	private static final Logger logger = LoggerFactory.getLogger(AlchemerSurveyCompletionController.class);

	private final SurveyRepository surveyRepository;
	private final PanelistRepository panelistRepository;
	private final SurveyPanelistParticipationRepository surveyPanelistParticipationRepository;
	private final TaskRepository taskRepository;
	private final ConfigurationItemRepository configurationItemRepository;

	public AlchemerSurveyCompletionController(SurveyRepository surveyRepository, PanelistRepository panelistRepository,
			SurveyPanelistParticipationRepository surveyPanelistParticipationRepository, TaskRepository taskRepository,
			ConfigurationItemRepository configurationItemRepository) {
		this.surveyRepository = surveyRepository;
		this.panelistRepository = panelistRepository;
		this.surveyPanelistParticipationRepository = surveyPanelistParticipationRepository;
		this.taskRepository = taskRepository;
		this.configurationItemRepository = configurationItemRepository;
	}

	@PostMapping("/survey-response")
    @Transactional // Important for database operations
    public ResponseEntity<String> handleSurveyResponse(@RequestBody AlchemerSurveyCompletionPayloadDto payload) {
        if (payload == null || payload.getData() == null || payload.getData().getContact() == null) {
            logger.warn("Received incomplete payload");
            return ResponseEntity.badRequest().body("Incomplete payload: 'data' or 'contact' field is missing.");
        }

        // Extract survey_id and convert to String for alchemerSurveyId
        String alchemerSurveyId = String.valueOf(payload.getData().getSurveyId());
        String email = payload.getData().getContact().getEmail();

        Optional<ConfigurationItem> recruitmentCampaignLink = configurationItemRepository.findByName("recruitment.alchemer.campaign.link");
        if (recruitmentCampaignLink.isPresent()) {
		String recruitmentSurveyId = extractSurveyId(recruitmentCampaignLink.get().getValue());
		if (recruitmentSurveyId != null && recruitmentSurveyId.equals(alchemerSurveyId)) {
			return confirmRecruitment(email);
		}
        }

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
        
        if(panelist.getLastInterviewCompleted()==null || panelist.getLastInterviewCompleted().isBefore(LocalDate.now())){
        	panelist.setLastInterviewCompleted(LocalDate.now());
        	this.panelistRepository.save(panelist);
        }
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

        
        participation.setResponseId(payload.getData().getResponseId());
        participation.setCompleted(true);
        participation.setDateCompleted(LocalDate.now()); // Using LocalDate as per entity definition

        surveyPanelistParticipationRepository.save(participation);

        logger.info("Successfully updated participation. Survey internal_id: {}, Panelist internal_id: {}. Marked as completed on {}",
                    survey.getId(), panelist.getId(), participation.getDateCompleted());

        // Create and save the new Task
        Task task = new Task();
        task.setJobType(JobType.ALCHEMER_ANSWER_RETRIEVAL);
        task.setCreated(LocalDateTime.now());
        task.setStatus(TaskStatus.PENDING);
        task.setSurveyPanelistParticipation(participation);
        task.setSurvey(survey); // survey object is already available from earlier in the method
        taskRepository.save(task);

        logger.info("Successfully created Task for answer retrieval. Task id: {}, Survey internal_id: {}, Panelist internal_id: {}",
                    task.getId(), survey.getId(), panelist.getId());

        return ResponseEntity.ok("Webhook processed successfully. Participation updated and task created.");
    }

    private ResponseEntity<String> confirmRecruitment(String email) {
        Optional<Panelist> panelistOpt = panelistRepository.findByEmail(email);
        if (!panelistOpt.isPresent()) {
            logger.warn("Panelist not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Panelist not found for email: " + email);
        }
        Panelist panelist = panelistOpt.get();
        panelist.setStatus(uy.com.equipos.panelmanagement.data.Status.ACTIVO);
        panelistRepository.save(panelist);
        logger.info("Panelist {} confirmed and set to ACTIVO", email);
        return ResponseEntity.ok("Panelist confirmed successfully.");
    }

	private String extractSurveyId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}

		// 1. Try the new URL pattern:
		// https://app.alchemer.com/invite/messages/id/SURVEY_ID/link/CAMPAIGN_ID
		// Example: "https://app.alchemer.com/invite/messages/id/8367882/link/24099873"
		// -> "8367882"
		java.util.regex.Pattern newUrlPattern = java.util.regex.Pattern.compile("/id/(\\d+)/link/(\\d+)");
		java.util.regex.Matcher newMatcher = newUrlPattern.matcher(surveyLink);
		if (newMatcher.find()) {
			return newMatcher.group(1); // SURVEY_ID
		}

		// 2. Try common older Alchemer URL pattern: e.g., /s3/SURVEY_ID/... or
		// /survey/SURVEY_ID/...
		// Example: "https://app.alchemer.com/s3/1234567/My-Survey" -> "1234567"
		java.util.regex.Pattern oldUrlPattern = java.util.regex.Pattern.compile("/(?:s3|survey)/(\\d+)");
		java.util.regex.Matcher oldMatcher = oldUrlPattern.matcher(surveyLink);
		if (oldMatcher.find()) {
			return oldMatcher.group(1); // SURVEY_ID
		}

		// 3. Check if the surveyLink itself is a plain numeric ID
		// Example: "8367882" -> "8367882"
		if (surveyLink.matches("\\d+")) {
			return surveyLink;
		}

		// 4. If no pattern matched and it's not a plain number, we couldn't extract a
		// survey ID.
		return null;
	}
}
