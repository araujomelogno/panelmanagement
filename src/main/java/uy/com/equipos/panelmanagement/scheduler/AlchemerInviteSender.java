package uy.com.equipos.panelmanagement.scheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.Task;
import uy.com.equipos.panelmanagement.data.TaskStatus;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation;
import uy.com.equipos.panelmanagement.services.TaskService;
import uy.com.equipos.panelmanagement.services.PanelistService;
import uy.com.equipos.panelmanagement.services.SurveyPanelistParticipationService;

@Component
@EnableScheduling
public class AlchemerInviteSender {

	private static final Logger log = LoggerFactory.getLogger(AlchemerInviteSender.class);

	private final TaskService messageTaskService;
	private final SurveyPanelistParticipationService surveyPanelistParticipationService;
	private final PanelistService panelistService;
	private final RestTemplate restTemplate;

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	private static final String ALCHEMER_API_BASE_URL = "https://api.alchemer.com";

	public AlchemerInviteSender(TaskService messageTaskService,
			SurveyPanelistParticipationService surveyPanelistParticipationService, PanelistService panelistService) {
		this.messageTaskService = messageTaskService;
		this.surveyPanelistParticipationService = surveyPanelistParticipationService;
		this.panelistService = panelistService;
		this.restTemplate = new RestTemplate();
	}

	 @Scheduled(cron = "*/30 * * * * *")
//	@Scheduled(cron = "0 */5 * * * *")
	public void sendInvites() {
		log.info("Iniciando tarea AlchemerInviteSender");
		List<Task> pendingTasks = messageTaskService.findAllByJobTypeAndStatus(JobType.ALCHEMER_INVITE,
				TaskStatus.PENDING);

		for (Task task : pendingTasks) {
			try {
				log.info("Procesando MessageTask ID: {}", task.getId());
				Optional<SurveyPanelistParticipation> participationOpt = surveyPanelistParticipationService
						.get(task.getSurveyPanelistParticipation().getId());

				if (participationOpt.isEmpty()) {
					log.error("No se encontró SurveyPanelistParticipation para MessageTask ID: {}", task.getId());
					task.setStatus(TaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}

				SurveyPanelistParticipation participation = participationOpt.get();
				if (participation.getSurvey() == null || participation.getSurvey().getLink() == null) {
					log.error("Survey o Survey Link no encontrado para Participation ID: {}", participation.getId());
					task.setStatus(TaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}
				String surveyLink = participation.getSurvey().getLink();

				if (participation.getPanelist() == null) {
					log.error("Panelist no encontrado para Participation ID: {}", participation.getId());
					task.setStatus(TaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}
				Panelist panelist = participation.getPanelist();
				String email = panelist.getEmail();
				String firstName = panelist.getFirstName();
				String lastName = panelist.getLastName();

				// 6. Revisar si el email recuperado esta en la campaña
				String contactId = getContactId(surveyLink, email);

				// 7. si no existe el email en la campaña , se deberá agregar el contacto.
				if (contactId == null) {
					contactId = addContactToCampaign(surveyLink, email, firstName, lastName);
					if (contactId == null) {
						log.error(
								"No se pudo obtener o crear el ContactID para el email: {} en la campaña del survey link: {}",
								email, surveyLink);
						task.setStatus(TaskStatus.ERROR);
						messageTaskService.save(task);
						continue;
					}

				}
				// 8. Enviar la invitación (solo envia si no lo envio antes)
				boolean invitationSent = sendInvitation(surveyLink, contactId);

				// 9. Si la invocación del paso anterior es exitosa
				if (invitationSent) {
					if (panelist.getLastContacted() == null) {
						panelist.setLastContacted(LocalDate.now());
						panelistService.save(panelist);
					}

					if (participation.getDateSent() == null) {
						participation.setDateSent(LocalDate.now());
						surveyPanelistParticipationService.save(participation);
					}

					task.setStatus(TaskStatus.DONE);
					messageTaskService.save(task);
					log.info("Invitación enviada exitosamente para MessageTask ID: {}", task.getId());
				} else {
					log.error("Error al enviar la invitación para MessageTask ID: {}", task.getId());
					task.setStatus(TaskStatus.ERROR);
					messageTaskService.save(task);
				}

			} catch (Exception e) {
				log.error("Error procesando MessageTask ID: {}. Error: {}", task.getId(), e.getMessage(), e);
				task.setStatus(TaskStatus.ERROR);
				messageTaskService.save(task);
			}
		}
		log.info("Finalizada tarea AlchemerInviteSender. Tareas procesadas: {}", pendingTasks.size());
	}

	private String getContactId(String surveyLinkOrSurveyId, String email) {
		String surveyId = extractSurveyId(surveyLinkOrSurveyId);
		String campaignId = extractCampaignId(surveyLinkOrSurveyId);

		if (surveyId == null) {
			log.error("getContactId: Could not extract Survey ID from input: {}", surveyLinkOrSurveyId);
			return null;
		}
		if (campaignId == null) {
			log.warn(
					"getContactId: Campaign ID could not be extracted from input: {}. Defaulting to Survey ID for campaign context.",
					surveyLinkOrSurveyId);
			campaignId = surveyId; // Default to surveyId if campaignId is not specifically found
		}

		// V5 SurveyContact API: GET
		// /v5/survey/{survey_id}/surveycampaign/{campaign_id}/surveycontact
		// Filter by email:
		// ?filter[field][0]=email_address&filter[operator][0]==&filter[value][0]={email}

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "surveycontact")
				.queryParam("filter[field][0]", "semailaddress").queryParam("filter[operator][0]", "=")
				.queryParam("filter[value][0]", email) // email value will be URL encoded by the builder
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret);

		String url = builder.toUriString();
		log.debug("getContactId URL: {}", url);

		try {

			ResponseEntity<Map> response = restTemplate.getForEntity(builder.build().toUri(), Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("getContactId response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					Object dataObj = response.getBody().get("data");
					if (dataObj instanceof List) {
						List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
						if (!dataList.isEmpty()) {
							Map<String, Object> contactData = dataList.get(0);
							log.info("Contacto encontrado para email {}: ID {} (SurveyID: {}, CampaignID: {})", email,
									contactData.get("id"), surveyId, campaignId);
							return String.valueOf(contactData.get("id"));
						} else {
							log.info(
									"Contacto con email {} no encontrado en SurveyID: {}, CampaignID: {} (lista vacía).",
									email, surveyId, campaignId);
						}
					} else if (dataObj instanceof Map) {
						Map<String, Object> contactData = (Map<String, Object>) dataObj;
						if (contactData.containsKey("id")
								&& email.equalsIgnoreCase(String.valueOf(contactData.get("email_address")))) {
							log.info("Contacto encontrado para email {}: ID {} (SurveyID: {}, CampaignID: {})", email,
									contactData.get("id"), surveyId, campaignId);
							return String.valueOf(contactData.get("id"));
						}
					} else {
						log.info(
								"Contacto con email {} no encontrado en SurveyID: {}, CampaignID: {} (formato de 'data' inesperado). Respuesta: {}",
								email, surveyId, campaignId, response.getBody());
					}
				} else {
					log.warn(
							"API call para getContactId no fue exitosa (result_ok=false) for email {}. SurveyID: {}, CampaignID: {}. Respuesta: {}",
							email, surveyId, campaignId, response.getBody());
				}
			} else {
				log.warn("Respuesta no OK del servidor ({}) al buscar contacto {} en SurveyID: {}, CampaignID: {}.",
						response.getStatusCode(), email, surveyId, campaignId);
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.info("Contacto {} no encontrado (HTTP 404) en SurveyID: {}, CampaignID: {}.", email, surveyId,
						campaignId);
			} else {
				log.error(
						"Error (HttpClientErrorException) al verificar contacto {} en SurveyID: {}, CampaignID: {}: {} - {}",
						email, surveyId, campaignId, e.getStatusCode(), e.getResponseBodyAsString());
			}
		} catch (org.springframework.web.client.RestClientException e) {
			log.error("Error (RestClientException) al verificar contacto {} en SurveyID: {}, CampaignID: {}: {}", email,
					surveyId, campaignId, e.getMessage(), e);
		}
		return null;
	}

	private String addContactToCampaign(String surveyLinkOrSurveyId, String email, String firstName, String lastName) {
		String surveyId = extractSurveyId(surveyLinkOrSurveyId);
		String campaignId = extractCampaignId(surveyLinkOrSurveyId);

		if (surveyId == null) {
			log.error("addContactToCampaign: Could not extract Survey ID from input: {}", surveyLinkOrSurveyId);
			return null;
		}
		if (campaignId == null) {
			log.warn(
					"addContactToCampaign: Campaign ID could not be extracted from input: {}. Defaulting to Survey ID for campaign context.",
					surveyLinkOrSurveyId);
			campaignId = surveyId; // Default to surveyId
		}

		// V5 SurveyContact CREATE: PUT
		// /v5/survey/{s_id}/surveycampaign/{c_id}/surveycontact
		// Contact data as URL parameters.
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "surveycontact")
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret)
				.queryParam("email_address", email) // Parameter names from Alchemer docs
				.queryParam("first_name", firstName).queryParam("last_name", lastName);

		String url = builder.toUriString();
		log.debug("addContactToCampaign URL: {}", url);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);

		try {
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("addContactToCampaign response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {

					String newContactId = response.getBody().get("id").toString();
					return newContactId;

				} else {
					log.error(
							"Error en la respuesta de Alchemer (result_ok false) al agregar contacto {} (SurveyID: {}, CampaignID: {}): {}",
							email, surveyId, campaignId, response.getBody());
				}
			} else {
				log.error("Error del servidor ({}) al agregar contacto {} a SurveyID: {}, CampaignID: {}. Body: {}",
						response.getStatusCode(), email, surveyId, campaignId, response.getBody());
			}
		} catch (

		HttpClientErrorException e) {
			log.error("Error (HttpClientErrorException) al agregar contacto {} a SurveyID: {}, CampaignID: {}: {} - {}",
					email, surveyId, campaignId, e.getStatusCode(), e.getResponseBodyAsString());
		} catch (org.springframework.web.client.RestClientException e) {
			log.error("Error (RestClientException) al agregar contacto {} a SurveyID: {}, CampaignID: {}: {}", email,
					surveyId, campaignId, e.getMessage(), e);
		}
		return null;
	}

	private String getEmailMessageIdToSend(String surveyId, String campaignId) {
		// Construct URL for GET
		// /v5/survey/{surveyId}/surveycampaign/{campaignId}/emailmessage
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "emailmessage")
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret);

		String url = builder.toUriString();
		log.debug("getEmailMessageIdToSend URL: {}", url);

		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("getEmailMessageIdToSend response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					Object dataObj = response.getBody().get("data");
					if (dataObj instanceof List) {
						@SuppressWarnings("unchecked") // Checked by instanceof
						List<Map<String, Object>> messages = (List<Map<String, Object>>) dataObj;
						for (Map<String, Object> message : messages) {
							// Look for the first message of subtype "message" (initial invite)
							if ("message".equalsIgnoreCase(String.valueOf(message.get("subtype")))) {
								String messageId = String.valueOf(message.get("id"));
								log.info("Found EmailMessage ID {} of subtype 'message' for CampaignID {}", messageId,
										campaignId);
								return messageId;
							}
						}
						log.warn("No EmailMessage with subtype 'message' found for CampaignID {}", campaignId);
					} else {
						log.warn("EmailMessage list not found or in unexpected format for CampaignID {}. Response: {}",
								campaignId, response.getBody());
					}
				} else {
					log.warn(
							"API call for getEmailMessageIdToSend was not successful (result_ok=false) for CampaignID {}. Response: {}",
							campaignId, response.getBody());
				}
			} else {
				log.warn("Server error ({}) while fetching email messages for CampaignID {}", response.getStatusCode(),
						campaignId);
			}
		} catch (HttpClientErrorException e) {
			log.error("Error (HttpClientErrorException) fetching email messages for CampaignID {}: {} - {}", campaignId,
					e.getStatusCode(), e.getResponseBodyAsString(), e);
		} catch (org.springframework.web.client.RestClientException e) {
			log.error("Error (RestClientException) fetching email messages for CampaignID {}: {}", campaignId,
					e.getMessage(), e);
		}
		return null;
	}

	private boolean sendInvitation(String surveyLinkOrSurveyId, String contactId) { // contactId is no longer directly
																					// used in the API call here but
																					// good for logging
		String surveyId = extractSurveyId(surveyLinkOrSurveyId);
		String campaignId = extractCampaignId(surveyLinkOrSurveyId);

		if (surveyId == null) {
			log.error("sendInvitation: Could not extract Survey ID from input: {}", surveyLinkOrSurveyId);
			return false;
		}
		if (campaignId == null) {
			// Fallback for campaignId if not directly found in link (though
			// extractCampaignId should handle most cases)
			log.warn(
					"sendInvitation: Campaign ID not directly found via extractCampaignId for input: {}. Using Survey ID {} as fallback campaign context.",
					surveyLinkOrSurveyId, surveyId);
			campaignId = surveyId; // Should ideally not happen if extractCampaignId is robust
		}

		String messageId = getEmailMessageIdToSend(surveyId, campaignId);
		if (messageId == null) {
			log.error(
					"sendInvitation: Could not retrieve an EmailMessage ID to send for SurveyID {} and CampaignID {}. ContactID {} will not be invited at this time.",
					surveyId, campaignId, contactId);
			return false;
		}

		// UPDATE EmailMessage with send=true
		// POST
		// /v5/survey/{surveyId}/surveycampaign/{campaignId}/emailmessage/{messageId}?send=true
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "emailmessage", messageId)
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret)
				.queryParam("send", "true"); // Parameter to trigger the send
		// The Alchemer docs for EmailMessage Update show _method=POST as a query param:
		// https://apihelp.alchemer.com/help/emailmessage-sub-object-v5#updateobject
		// ".../emailmessage/100000?_method=POST"
		// It's safer to include it if their server relies on it for routing or
		// processing.
		builder.queryParam("_method", "POST");

		String url = builder.toUriString();
		log.info("sendInvitation (Update EmailMessage to send) URL: {}", url); // Changed to INFO for better visibility

		HttpHeaders headers = new HttpHeaders();
		// Parameters are in the URL, so body can be empty.
		// Content-Type for POSTs with URL parameters is typically
		// application/x-www-form-urlencoded
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<String> requestEntity = new HttpEntity<>(null, headers); // Empty body

		try {
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("sendInvitation (Update EmailMessage) response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					log.info(
							"Invitation for ContactID {} via EmailMessageID {} in CampaignID {} successfully triggered for sending. SurveyID: {}",
							contactId, messageId, campaignId, surveyId);
					return true;
				} else {
					log.error(
							"API error (result_ok=false) when triggering send for EmailMessageID {} (CampaignID {}). ContactID {}. Response: {}",
							messageId, campaignId, contactId, response.getBody());
					return false;
				}
			} else {
				log.error(
						"Server error ({}) when triggering send for EmailMessageID {} (CampaignID {}). ContactID {}. Body: {}",
						response.getStatusCode(), messageId, campaignId, contactId, response.getBody());
			}
		} catch (HttpClientErrorException e) {
			log.error(
					"Error (HttpClientErrorException) triggering send for EmailMessageID {} (CampaignID {}). ContactID {}: {} - {}",
					messageId, campaignId, contactId, e.getStatusCode(), e.getResponseBodyAsString(), e);
		} catch (org.springframework.web.client.RestClientException e) {
			log.error(
					"Error (RestClientException) triggering send for EmailMessageID {} (CampaignID {}). ContactID {}: {}",
					messageId, campaignId, contactId, e.getMessage(), e);
		}
		return false;
	}

	private String extractCampaignId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}

		// Pattern for the new URL format:
		// https://app.alchemer.com/invite/messages/id/SURVEY_ID/link/CAMPAIGN_ID
		// We are interested in the CAMPAIGN_ID, which is the last numeric part.
		// Example: "https://app.alchemer.com/invite/messages/id/8367882/link/24099873"
		// -> "24099873"
		Pattern newUrlPattern = Pattern.compile("/id/(\\d+)/link/(\\d+)$");
		Matcher matcher = newUrlPattern.matcher(surveyLink);

		if (matcher.find()) {
			// Group 2 is the CAMPAIGN_ID
			return matcher.group(2);
		}

		// Fallback to existing logic for other URL formats or direct IDs
		if (surveyLink.contains("/")) {
			String[] parts = surveyLink.split("/");
			for (int i = parts.length - 1; i >= 0; i--) {
				// Check if the part is purely numeric and not empty
				if (!parts[i].isEmpty() && parts[i].matches("\\d+")) {
					return parts[i];
				}
			}
		}
		// If not a recognized URL pattern or no numeric part found by fallback,
		// and it's not null and doesn't contain '/', assume it's the ID directly.
		// Also, if it contained '/' but no numeric part was extracted, it might be a
		// malformed URL
		// or a direct ID with an accidental slash. If it's numeric, treat as ID.
		if (surveyLink.matches("\\d+")) {
			return surveyLink;
		}
		// If no ID could be parsed, return null.
		return null;
	}

	private String extractSurveyId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}

		// 1. Try the new URL pattern:
		// https://app.alchemer.com/invite/messages/id/SURVEY_ID/link/CAMPAIGN_ID
		// Example: "https://app.alchemer.com/invite/messages/id/8367882/link/24099873"
		// -> "8367882"
		Pattern newUrlPattern = Pattern.compile("/id/(\\d+)/link/(\\d+)");
		Matcher newMatcher = newUrlPattern.matcher(surveyLink);
		if (newMatcher.find()) {
			return newMatcher.group(1); // SURVEY_ID
		}

		// 2. Try common older Alchemer URL pattern: e.g., /s3/SURVEY_ID/... or
		// /survey/SURVEY_ID/...
		// Example: "https://app.alchemer.com/s3/1234567/My-Survey" -> "1234567"
		Pattern oldUrlPattern = Pattern.compile("/(?:s3|survey)/(\\d+)");
		Matcher oldMatcher = oldUrlPattern.matcher(surveyLink);
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
