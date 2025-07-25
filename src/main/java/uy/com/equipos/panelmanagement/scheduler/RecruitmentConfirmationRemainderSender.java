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

import uy.com.equipos.panelmanagement.data.ConfigurationItem;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.Status;
import uy.com.equipos.panelmanagement.services.ConfigurationItemService;
import uy.com.equipos.panelmanagement.services.PanelistService;

@Component
@EnableScheduling
public class RecruitmentConfirmationRemainderSender {

	private static final Logger log = LoggerFactory.getLogger(RecruitmentConfirmationRemainderSender.class);

	private final PanelistService panelistService;
	private final RestTemplate restTemplate;
	private final ConfigurationItemService configurationItemService;

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	private static final String ALCHEMER_API_BASE_URL = "https://api.alchemer.com";

	public RecruitmentConfirmationRemainderSender(PanelistService panelistService,
			ConfigurationItemService configurationItemService) {
		this.panelistService = panelistService;
		this.configurationItemService = configurationItemService;
		this.restTemplate = new RestTemplate();
	}

	@Scheduled(cron = "0 0 10 * * *")
	public void sendRecruitmentConfirmationRemainder() {
		log.info("Iniciando tarea RecruitmentConfirmationRemainderSender");
		List<Panelist> pendingPanelists = panelistService.findByStatus(Status.PENDIENTE);

		Optional<ConfigurationItem> campaignLinkItem = configurationItemService
				.getByName("recruitment.alchemer.campaign.link");
		if (campaignLinkItem.isEmpty()) {
			log.error("No se encontró el item de configuracion 'recruitment.alchemer.campaign.link'");
			return;
		}
		String campaignLink = campaignLinkItem.get().getValue();

		String surveyId = extractSurveyId(campaignLink);
		String campaignId = extractCampaignId(campaignLink);

		if (surveyId == null || campaignId == null) {
			log.error("No se pudo extraer SurveyID o CampaignID del link {}", campaignLink);
			return;
		}

		String emailMessageId = getReminderEmailMessageId(surveyId, campaignId);
		if (emailMessageId == null) {
			log.error("No se pudo obtener EmailMessageID de tipo 'reminder' para SurveyID: {}, CampaignID: {}.",
					surveyId, campaignId);
			return;
		}

		Optional<ConfigurationItem> recruitmentRetryItem = configurationItemService.getByName("recruitment.retry");
		if (recruitmentRetryItem.isEmpty()) {
			log.error("No se encontró el item de configuracion 'recruitment.retry'");
			return;
		}
		Integer recruitmentRetry = Integer.parseInt(recruitmentRetryItem.get().getValue());
		sendReminderEmail(surveyId, campaignId, emailMessageId);
		for (Panelist panelist : pendingPanelists) {
			panelist.setLastRecruitmentSent(LocalDate.now());
			panelist.setRecruitmentRetries(panelist.getRecruitmentRetries() + 1);
			if (panelist.getRecruitmentRetries() > recruitmentRetry) {
				panelist.setStatus(Status.INACTIVO);
				String contactId = getContactId(campaignLink, panelist.getEmail());
				if (contactId != null) {
					deleteContact(surveyId, campaignId, contactId);
				}
			}
			panelistService.save(panelist);
		}
		log.info("Finalizada tarea RecruitmentConfirmationRemainderSender. Tareas procesadas: {}",
				pendingPanelists.size());
	}

	private void deleteContact(String surveyId, String campaignId, String contactId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "surveycontact", contactId)
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret)
				.queryParam("_method", "DELETE");

		String url = builder.toUriString();
		log.info("deleteContact URL: {}", url);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

		try {
			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("deleteContact response body: {}", response.getBody());
				if (Boolean.TRUE.equals(response.getBody().get("result_ok"))) {
					log.info("Contacto: " + contactId + " eliminado exitosamente");
				} else {
					log.error("API error (result_ok=false) when deleting contact {} (CampaignID {}). Response: {}",
							contactId, campaignId, response.getBody());
				}
			} else {
				log.error("Server error ({}) when deleting contact {} (CampaignID {}). Body: {}",
						response.getStatusCode(), contactId, campaignId, response.getBody());
			}
		} catch (HttpClientErrorException e) {
			log.error("Error (HttpClientErrorException) deleting contact {} (CampaignID {}): {} - {}", contactId,
					campaignId, e.getStatusCode(), e.getResponseBodyAsString(), e);
		} catch (org.springframework.web.client.RestClientException e) {
			log.error("Error (RestClientException) deleting contact {} (CampaignID {}): {}", contactId, campaignId,
					e.getMessage(), e);
		}
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
			campaignId = surveyId;
		}

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "surveycontact")
				.queryParam("filter[field][0]", "semailaddress").queryParam("filter[operator][0]", "=")
				.queryParam("filter[value][0]", email).queryParam("api_token", apiToken)
				.queryParam("api_token_secret", apiTokenSecret);

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

	private boolean sendReminderEmail(String surveyId, String campaignId, String emailMessageId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
				.pathSegment("v5", "survey", surveyId, "surveycampaign", campaignId, "emailmessage", emailMessageId)
				.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret)
				.queryParam("_method", "POST").queryParam("send", "true");

		String url = builder.toUriString();
		log.info("sendReminderEmail URL: {}", url);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

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

	private String extractCampaignId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}
		Pattern pattern = Pattern.compile("/id/(\\d+)/link/(\\d+)$");
		Matcher matcher = pattern.matcher(surveyLink);
		if (matcher.find()) {
			return matcher.group(2);
		}
		log.warn("No se pudo extraer CampaignID del link {} con el patrón esperado.", surveyLink);
		return null;
	}

	private String extractSurveyId(String surveyLink) {
		if (surveyLink == null) {
			return null;
		}
		Pattern pattern = Pattern.compile("/id/(\\d+)/link/(\\d+)");
		Matcher matcher = pattern.matcher(surveyLink);
		if (matcher.find()) {
			return matcher.group(1);
		}
		log.warn("No se pudo extraer SurveyID del link {} con el patrón esperado.", surveyLink);
		return null;
	}
}
