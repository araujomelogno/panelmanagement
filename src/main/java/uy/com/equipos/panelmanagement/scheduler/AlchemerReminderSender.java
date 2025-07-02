package uy.com.equipos.panelmanagement.scheduler;

import java.util.List;
import java.util.Map;
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
import uy.com.equipos.panelmanagement.data.MessageTask;
import uy.com.equipos.panelmanagement.data.MessageTaskStatus;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.services.MessageTaskService;

@Component
@EnableScheduling
public class AlchemerReminderSender {

	private static final Logger log = LoggerFactory.getLogger(AlchemerReminderSender.class);

	private final MessageTaskService messageTaskService;
	private final RestTemplate restTemplate;

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	private static final String ALCHEMER_API_BASE_URL = "https://api.alchemer.com";

	public AlchemerReminderSender(MessageTaskService messageTaskService) {
		this.messageTaskService = messageTaskService;
		this.restTemplate = new RestTemplate();
	}

	// @Scheduled(cron = "0 */5 * * * *")
	@Scheduled(cron = "30 * * * * *")
	public void sendReminders() {
		log.info("Iniciando tarea AlchemerReminderSender");
		List<MessageTask> pendingTasks = messageTaskService.findAllByJobTypeAndStatus(JobType.ALCHEMER_REMINDER,
				MessageTaskStatus.PENDING);

		log.info("Se encontraron {} tareas pendientes de envío de recordatorios.", pendingTasks.size());

		for (MessageTask task : pendingTasks) {
			try {
				log.info("Procesando MessageTask ID: {} para envío de recordatorio.", task.getId());
				Survey survey = task.getSurvey();
				if (survey == null || survey.getLink() == null || survey.getLink().isEmpty()) {
					log.error("Survey o Survey Link no encontrado para MessageTask ID: {}", task.getId());
					task.setStatus(MessageTaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}
				String surveyLink = survey.getLink();
				log.debug("Survey Link obtenido: {}", surveyLink);

				String surveyId = extractSurveyId(surveyLink);
				String campaignId = extractCampaignId(surveyLink);

				if (surveyId == null || campaignId == null) {
					log.error("No se pudo extraer SurveyID o CampaignID del link {} para MessageTask ID: {}",
							surveyLink, task.getId());
					task.setStatus(MessageTaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}
				log.info("Extraído SurveyID: {} y CampaignID: {} para MessageTask ID: {}", surveyId, campaignId,
						task.getId());

				String emailMessageId = getReminderEmailMessageId(surveyId, campaignId);

				if (emailMessageId == null) {
					log.error(
							"No se pudo obtener EmailMessageID de tipo 'reminder' para SurveyID: {}, CampaignID: {}. MessageTask ID: {}",
							surveyId, campaignId, task.getId());
					task.setStatus(MessageTaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}
				log.info("EmailMessageID de recordatorio obtenido: {} para SurveyID: {}, CampaignID: {}",
						emailMessageId, surveyId, campaignId);

				boolean reminderSent = sendReminderEmail(surveyId, campaignId, emailMessageId);

				if (reminderSent) {
					log.info("Recordatorio enviado exitosamente para MessageTask ID: {}", task.getId());
					task.setStatus(MessageTaskStatus.DONE);
				} else {
					log.error("Error al enviar el recordatorio para MessageTask ID: {}", task.getId());
					task.setStatus(MessageTaskStatus.ERROR);
				}
				messageTaskService.save(task);
			} catch (Exception e) {
				log.error("Error procesando MessageTask ID: {}. Error: {}", task.getId(), e.getMessage(), e);
				task.setStatus(MessageTaskStatus.ERROR);
				messageTaskService.save(task);
			}
		}
		log.info("Finalizada tarea AlchemerReminderSender. Tareas procesadas: {}", pendingTasks.size());
	}

	// Los métodos para interactuar con la API de Alchemer (extraer IDs, obtener
	// emailMessageId, enviar recordatorio)
	// se agregarán en los siguientes pasos del plan.

	private String extractCampaignId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}
		// Formato:
		// https://app.alchemer.com/invite/messages/id/SURVEY_ID/link/CAMPAIGN_ID
		Pattern pattern = Pattern.compile("/id/(\\d+)/link/(\\d+)$");
		Matcher matcher = pattern.matcher(surveyLink);
		if (matcher.find()) {
			return matcher.group(2); // CAMPAIGN_ID
		}
		// Fallback por si el link es solo el campaignId o un formato antiguo no
		// esperado aquí
		// pero el AlchemerInviteSender tiene una lógica más robusta que podríamos
		// replicar si es necesario.
		// Por ahora, nos enfocamos en el formato explícito.
		log.warn("No se pudo extraer CampaignID del link {} con el patrón esperado.", surveyLink);
		return null;
	}

	private String extractSurveyId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}
		// Formato:
		// https://app.alchemer.com/invite/messages/id/SURVEY_ID/link/CAMPAIGN_ID
		Pattern pattern = Pattern.compile("/id/(\\d+)/link/(\\d+)");
		Matcher matcher = pattern.matcher(surveyLink);
		if (matcher.find()) {
			return matcher.group(1); // SURVEY_ID
		}
		// Fallback similar al de campaignId
		log.warn("No se pudo extraer SurveyID del link {} con el patrón esperado.", surveyLink);
		return null;
	}

	private String getReminderEmailMessageId(String surveyId, String campaignId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "emailmessage")
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret);

		String url = builder.toUriString();
		log.debug("getReminderEmailMessageId URL: {}", url);

		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("getReminderEmailMessageId response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					Object dataObj = response.getBody().get("data");
					if (dataObj instanceof List) {
						@SuppressWarnings("unchecked")
						List<Map<String, Object>> messages = (List<Map<String, Object>>) dataObj;
						for (Map<String, Object> message : messages) {
							if ("reminder".equalsIgnoreCase(String.valueOf(message.get("subtype")))) {
								String messageId = String.valueOf(message.get("id"));
								log.info("Found EmailMessage ID {} of subtype 'reminder' for CampaignID {}", messageId,
										campaignId);
								return messageId;
							}
						}
						log.warn("No EmailMessage with subtype 'reminder' found for CampaignID {}", campaignId);
					} else {
						log.warn("EmailMessage list not found or in unexpected format for CampaignID {}. Response: {}",
								campaignId, response.getBody());
					}
				} else {
					log.warn(
							"API call for getReminderEmailMessageId was not successful (result_ok=false) for CampaignID {}. Response: {}",
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

	private boolean sendReminderEmail(String surveyId, String campaignId, String emailMessageId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "emailmessage", emailMessageId)
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret)
				.queryParam("_method", "POST") // Alchemer specific way to indicate a POST through query params for this
												// endpoint
				.queryParam("send", "true"); // Parameter to trigger the send

		String url = builder.toUriString();
		log.info("sendReminderEmail URL: {}", url);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // As per Alchemer docs for similar operations
		HttpEntity<String> requestEntity = new HttpEntity<>(null, headers); // Body can be empty as params are in URL

		try {
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("sendReminderEmail response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					log.info(
							"Reminder for panelist {} via EmailMessageID {} in CampaignID {} successfully triggered for sending. SurveyID: {}",
							emailMessageId, campaignId, surveyId);
					return true;
				} else {
					log.error(
							"API error (result_ok=false) when triggering send for EmailMessageID {} (CampaignID {}). Panelist {}. Response: {}",
							emailMessageId, campaignId, response.getBody());
					return false;
				}
			} else {
				log.error(
						"Server error ({}) when triggering send for EmailMessageID {} (CampaignID {}). Panelist {}. Body: {}",
						response.getStatusCode(), emailMessageId, campaignId, response.getBody());
			}
		} catch (HttpClientErrorException e) {
			log.error(
					"Error (HttpClientErrorException) triggering send for EmailMessageID {} (CampaignID {}). Panelist {}: {} - {}",
					emailMessageId, campaignId, e.getStatusCode(), e.getResponseBodyAsString(), e);
		} catch (org.springframework.web.client.RestClientException e) {
			log.error(
					"Error (RestClientException) triggering send for EmailMessageID {} (CampaignID {}). Panelist {}: {}",
					emailMessageId, campaignId, e.getMessage(), e);
		}
		return false;
	}
}
