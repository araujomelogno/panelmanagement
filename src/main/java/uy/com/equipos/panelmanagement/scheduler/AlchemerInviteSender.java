package uy.com.equipos.panelmanagement.scheduler;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation;
import uy.com.equipos.panelmanagement.services.MessageTaskService;
import uy.com.equipos.panelmanagement.services.PanelistService;
import uy.com.equipos.panelmanagement.services.SurveyPanelistParticipationService;

@Component
@EnableScheduling
public class AlchemerInviteSender {

	private static final Logger log = LoggerFactory.getLogger(AlchemerInviteSender.class);

	private final MessageTaskService messageTaskService;
	private final SurveyPanelistParticipationService surveyPanelistParticipationService;
	private final PanelistService panelistService;
	private final RestTemplate restTemplate;

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	private static final String ALCHEMER_API_BASE_URL = "https://api.alchemer.com";

	public AlchemerInviteSender(MessageTaskService messageTaskService,
			SurveyPanelistParticipationService surveyPanelistParticipationService, PanelistService panelistService) {
		this.messageTaskService = messageTaskService;
		this.surveyPanelistParticipationService = surveyPanelistParticipationService;
		this.panelistService = panelistService;
		this.restTemplate = new RestTemplate();
	}

	@Scheduled(cron = "*/30 * * * * *")
	public void sendInvites() {
		log.info("Iniciando tarea AlchemerInviteSender");
		List<MessageTask> pendingTasks = messageTaskService.findAllByJobTypeAndStatus(JobType.ALCHEMER_INVITE,
				MessageTaskStatus.PENDING);

		for (MessageTask task : pendingTasks) {
			try {
				log.info("Procesando MessageTask ID: {}", task.getId());
				Optional<SurveyPanelistParticipation> participationOpt = surveyPanelistParticipationService
						.get(task.getSurveyPanelistParticipation().getId());

				if (participationOpt.isEmpty()) {
					log.error("No se encontró SurveyPanelistParticipation para MessageTask ID: {}", task.getId());
					task.setStatus(MessageTaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}

				SurveyPanelistParticipation participation = participationOpt.get();
				if (participation.getSurvey() == null || participation.getSurvey().getLink() == null) {
					log.error("Survey o Survey Link no encontrado para Participation ID: {}", participation.getId());
					task.setStatus(MessageTaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}
				String surveyLink = participation.getSurvey().getLink();

				if (participation.getPanelist() == null) {
					log.error("Panelist no encontrado para Participation ID: {}", participation.getId());
					task.setStatus(MessageTaskStatus.ERROR);
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
				}

				if (contactId == null) {
					log.error(
							"No se pudo obtener o crear el ContactID para el email: {} en la campaña del survey link: {}",
							email, surveyLink);
					task.setStatus(MessageTaskStatus.ERROR);
					messageTaskService.save(task);
					continue;
				}

				// 8. Enviar la invitación
				boolean invitationSent = sendInvitation(surveyLink, contactId);

				// 9. Si la invocación del paso anterior es exitosa
				if (invitationSent) {
					panelist.setLastContacted(LocalDate.now());
					panelistService.save(panelist);

					participation.setDateSent(LocalDate.now());
					surveyPanelistParticipationService.save(participation);

					task.setStatus(MessageTaskStatus.DONE);
					messageTaskService.save(task);
					log.info("Invitación enviada exitosamente para MessageTask ID: {}", task.getId());
				} else {
					log.error("Error al enviar la invitación para MessageTask ID: {}", task.getId());
					task.setStatus(MessageTaskStatus.ERROR);
					messageTaskService.save(task);
				}

			} catch (Exception e) {
				log.error("Error procesando MessageTask ID: {}. Error: {}", task.getId(), e.getMessage(), e);
				task.setStatus(MessageTaskStatus.ERROR);
				messageTaskService.save(task);
			}
		}
		log.info("Finalizada tarea AlchemerInviteSender. Tareas procesadas: {}", pendingTasks.size());
	}

	private String getContactId(String surveyLinkOrSurveyId, String email) {
		String extractedId = extractCampaignId(surveyLinkOrSurveyId); // Assumed to be survey_id and used as campaign_id
																		// for default campaign

		// V5 SurveyContact API: GET
		// /v5/survey/{survey_id}/surveycampaign/{campaign_id}/surveycontact
		// Filter by email:
		// ?filter[field][0]=email_address&filter[operator][0]==&filter[value][0]={email}

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", extractedId, "surveycampaign", extractedId, "surveycontact")
				.queryParam("filter[field][0]", "email_address").queryParam("filter[operator][0]", "==")
				.queryParam("filter[value][0]", email) // email value will be URL encoded by the builder
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret);

		String url = builder.toUriString();
		log.debug("getContactId URL: {}", url);

		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("getContactId response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					Object dataObj = response.getBody().get("data");
					if (dataObj instanceof List) {
						List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
						if (!dataList.isEmpty()) {
							// Assuming the first contact found is the correct one
							Map<String, Object> contactData = dataList.get(0);
							log.info("Contacto encontrado para email {}: ID {}", email, contactData.get("id"));
							return String.valueOf(contactData.get("id"));
						} else {
							log.info("Contacto con email {} no encontrado en campaña {} (survey_id {}) (lista vacía).",
									email, extractedId, extractedId);
						}
					} else if (dataObj instanceof Map) {
						// Less common for list endpoints, but handle if API returns single object if
						// only one found
						Map<String, Object> contactData = (Map<String, Object>) dataObj;
						if (contactData.containsKey("id")
								&& email.equalsIgnoreCase(String.valueOf(contactData.get("email_address")))) {
							log.info("Contacto encontrado para email {}: ID {}", email, contactData.get("id"));
							return String.valueOf(contactData.get("id"));
						}
					} else {
						log.info(
								"Contacto con email {} no encontrado en campaña {} (survey_id {}) (formato de 'data' inesperado). Respuesta: {}",
								email, extractedId, extractedId, response.getBody());
					}
				} else {
					log.warn(
							"API call para getContactId no fue exitosa (result_ok=false) para email {}. Campaña/Survey ID {}. Respuesta: {}",
							email, extractedId, response.getBody());
				}
			} else {
				log.warn("Respuesta no OK del servidor ({}) al buscar contacto {} en campaña {} (survey_id {}).",
						response.getStatusCode(), email, extractedId, extractedId);
			}
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				log.info("Contacto {} no encontrado (HTTP 404) en campaña {} (survey_id {}).", email, extractedId,
						extractedId);
			} else {
				log.error(
						"Error (HttpClientErrorException) al verificar contacto {} en campaña {} (survey_id {}): {} - {}",
						email, extractedId, extractedId, e.getStatusCode(), e.getResponseBodyAsString());
			}
		} catch (org.springframework.web.client.RestClientException e) {
			log.error("Error (RestClientException) al verificar contacto {} en campaña {} (survey_id {}): {}", email,
					extractedId, extractedId, e.getMessage(), e);
		}
		return null;
	}

	private String addContactToCampaign(String surveyLinkOrSurveyId, String email, String firstName, String lastName) {
		String extractedId = extractCampaignId(surveyLinkOrSurveyId); // Assumed survey_id, used as campaign_id

		// V5 SurveyContact CREATE: PUT
		// /v5/survey/{s_id}/surveycampaign/{c_id}/surveycontact
		// Contact data as URL parameters.
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", extractedId, "surveycampaign", extractedId, "surveycontact")
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret)
				.queryParam("email_address", email) // Parameter names from Alchemer docs
				.queryParam("first_name", firstName).queryParam("last_name", lastName);
		// The Alchemer documentation for "CREATE CONTACT" for SurveyContact shows:
		// .../surveycontact/?_method=PUT&email_address=...
		// This implies that even if we make a PUT request, they might still expect
		// _method=PUT.
		// However, this is usually for clients that can't make true PUT requests.
		// For RestTemplate, a direct PUT should be fine. If issues arise, adding
		// _method=PUT can be tested.
		// builder.queryParam("_method", "PUT"); // Not adding this initially.

		String url = builder.toUriString();
		log.debug("addContactToCampaign URL: {}", url);

		HttpHeaders headers = new HttpHeaders();
		// For PUT with all data in URL parameters, the body is typically empty.
		// Alchemer docs don't specify Content-Type for this, but form-urlencoded is a
		// safe default if any is needed.
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers); // Empty body

		try {
			// Using exchange for PUT to get ResponseEntity back, enabling access to
			// response body and status.
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("addContactToCampaign response body: {}", response.getBody());
				// Expected response: { "result_ok": true, "data": { "id": "NEW_CONTACT_ID", ...
				// } }
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					Object dataObj = response.getBody().get("data");
					if (dataObj instanceof Map) {
						Map<String, Object> dataMap = (Map<String, Object>) dataObj;
						if (dataMap.containsKey("id")) {
							String newContactId = String.valueOf(dataMap.get("id"));
							log.info(
									"Contacto {} agregado/actualizado en campaña {} (survey_id {}). Nuevo ContactID: {}",
									email, extractedId, extractedId, newContactId);
							return newContactId;
						} else {
							log.error(
									"Respuesta OK pero sin ID de contacto al agregar {} a campaña {} (survey_id {}). Respuesta: {}",
									email, extractedId, extractedId, response.getBody());
						}
					} else {
						log.error("Formato de 'data' inesperado al agregar contacto {}. Respuesta: {}", email,
								response.getBody());
					}
				} else {
					log.error("Error en la respuesta de Alchemer (result_ok false) al agregar contacto {}: {}", email,
							response.getBody());
				}
			} else {
				log.error("Error del servidor ({}) al agregar contacto {} a campaña {} (survey_id {}). Body: {}",
						response.getStatusCode(), email, extractedId, extractedId, response.getBody());
			}
		} catch (HttpClientErrorException e) {
			log.error("Error (HttpClientErrorException) al agregar contacto {} a campaña {} (survey_id {}): {} - {}",
					email, extractedId, extractedId, e.getStatusCode(), e.getResponseBodyAsString());
		} catch (org.springframework.web.client.RestClientException e) {
			log.error("Error (RestClientException) al agregar contacto {} a campaña {} (survey_id {}): {}", email,
					extractedId, extractedId, e.getMessage(), e);
		}
		return null;
	}

	private boolean sendInvitation(String surveyLinkOrSurveyId, String contactId) {
		String extractedId = extractCampaignId(surveyLinkOrSurveyId); // Assumed survey_id, also used as campaign_id for
																		// the default/target campaign

		// The current code uses: POST /v5/surveycampaign/{campaignId}/send
		// with JSON body {"contact_ids": ["CONTACT_ID"]}.
		// This endpoint is not explicitly detailed in the main V5 object documentation
		// (SurveyCampaign, EmailMessage).
		// However, it might be a valid simplified endpoint for sending a default
		// campaign message.
		// We will proceed with this structure, ensuring correct IDs and improving
		// response handling.
		// If this proves incorrect, a more complex method involving specific
		// EmailMessage IDs would be needed.

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "surveycampaign", extractedId, "send").queryParam("api_token", apiToken)
				.queryParam("api_token_secret", apiTokenSecret);

		String url = builder.toUriString();
		log.debug("sendInvitation URL: {}", url);

		Map<String, List<String>> requestBody = new HashMap<>();
		// Sticking with "contact_ids" as per the original code. If issues occur,
		// "sps_contact_ids" could be an alternative to test based on some Alchemer
		// examples.
		requestBody.put("contact_ids", Collections.singletonList(contactId));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON); // Body is JSON
		HttpEntity<Map<String, List<String>>> requestEntity = new HttpEntity<>(requestBody, headers);

		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("sendInvitation response body: {}", response.getBody());
				// Successful response typically: {"result_ok":true,"data":{"QUEUED":"1"}}
				// Error example: {"result_ok":false,"message":"No valid contacts selected."}
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					// Further check data if necessary, e.g., data.QUEUED > 0
					// For now, result_ok:true is the primary success indicator.
					log.info(
							"Invitación para contacto {} en campaña {} (survey_id {}) procesada para envío. Respuesta: {}",
							contactId, extractedId, extractedId, response.getBody());
					return true;
				} else {
					log.error(
							"Error en la respuesta de Alchemer (result_ok false) al enviar invitación a ContactID {}, CampaignID/SurveyID {}. Respuesta: {}",
							contactId, extractedId, response.getBody());
					return false;
				}
			} else {
				log.error(
						"Error del servidor ({}) al enviar invitación a contacto {} en campaña {} (survey_id {}). Body: {}",
						response.getStatusCode(), contactId, extractedId, extractedId, response.getBody());
			}
		} catch (HttpClientErrorException e) {
			log.error(
					"Error (HttpClientErrorException) al enviar invitación a contacto {} en campaña {} (survey_id {}): {} - {}",
					contactId, extractedId, extractedId, e.getStatusCode(), e.getResponseBodyAsString());
		} catch (org.springframework.web.client.RestClientException e) {
			log.error("Error (RestClientException) al enviar invitación a contacto {} en campaña {} (survey_id {}): {}",
					contactId, extractedId, extractedId, e.getMessage(), e);
		}
		return false;
	}

	private String extractCampaignId(String surveyLink) {
		// Asume que el surveyLink es el ID de la campaña o contiene el ID.
		// Para la API de Alchemer, el {link obtenido en paso 3} se refiere al ID de la
		// encuesta,
		// y en el contexto de /surveycampaign/{campaign_id}/..., campaign_id es el ID
		// de la encuesta.
		// Si el 'link' es una URL completa, se necesitaría una lógica más robusta para
		// extraer el ID.
		// Por ahora, asumimos que 'link' es directamente el campaign_id (survey_id).
		// Ejemplo: si surveyLink es "1234567", se usa "1234567".
		// Si fuera una URL como "https://app.alchemer.com/s3/1234567/My-Survey", se
		// necesitaría extraer "1234567".
		// Esta implementación simple asume que el link es el ID.
		if (surveyLink != null && surveyLink.contains("/")) {
			// Intenta extraer el ID si es una URL simple, ej: /s3/1234567 o
			// /s3/1234567/something
			String[] parts = surveyLink.split("/");
			for (int i = parts.length - 1; i >= 0; i--) {
				if (parts[i].matches("\\d+")) { // Busca una secuencia de dígitos
					return parts[i];
				}
			}
		}
		// Si no es una URL o no se pudo extraer, se asume que es el ID directamente.
		return surveyLink;
	}
}
