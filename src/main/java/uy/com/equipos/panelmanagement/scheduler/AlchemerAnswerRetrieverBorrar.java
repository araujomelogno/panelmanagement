package uy.com.equipos.panelmanagement.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import uy.com.equipos.panelmanagement.data.AlchemerAnswer;
import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.PanelistPropertyValue;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation;
import uy.com.equipos.panelmanagement.data.SurveyPropertyMatching;
import uy.com.equipos.panelmanagement.data.Task;
import uy.com.equipos.panelmanagement.data.TaskStatus;
import uy.com.equipos.panelmanagement.services.AnswerService;
import uy.com.equipos.panelmanagement.services.PanelistPropertyValueService;
import uy.com.equipos.panelmanagement.services.SurveyPanelistParticipationService;
import uy.com.equipos.panelmanagement.services.SurveyPropertyMatchingService;
import uy.com.equipos.panelmanagement.services.TaskService;

@Component
@EnableScheduling
public class AlchemerAnswerRetrieverBorrar {

	private static final Logger log = LoggerFactory.getLogger(AlchemerAnswerRetrieverBorrar.class);

	private final TaskService taskService;
	private final SurveyPanelistParticipationService surveyPanelistParticipationService;
	private final AnswerService answerService;
	private final SurveyPropertyMatchingService surveyPropertyMatchingService;
	private final PanelistPropertyValueService panelistPropertyValueService;
	private final RestTemplate restTemplate;

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	private static final String ALCHEMER_API_BASE_URL = "https://api.alchemer.com/v5";

	public AlchemerAnswerRetrieverBorrar(TaskService taskService,
			SurveyPanelistParticipationService surveyPanelistParticipationService, AnswerService answerService,
			SurveyPropertyMatchingService surveyPropertyMatchingService,
			PanelistPropertyValueService panelistPropertyValueService) {
		this.taskService = taskService;
		this.surveyPanelistParticipationService = surveyPanelistParticipationService;
		this.answerService = answerService;
		this.surveyPropertyMatchingService = surveyPropertyMatchingService;
		this.panelistPropertyValueService = panelistPropertyValueService;
		this.restTemplate = new RestTemplate();
	}

	// @Scheduled(cron = "0 */5 * * * *") // Cada 5 minutos
	public void retrieveAnswers() {
		log.info("Iniciando tarea AlchemerAnswerRetriever");
		List<Task> pendingTasks = taskService.findAllByJobTypeAndStatus(JobType.ALCHEMER_ANSWER_RETRIEVAL,
				TaskStatus.PENDING);
		log.info("Se encontraron {} tareas pendientes de tipo ALCHEMER_ANSWER_RETRIEVAL", pendingTasks.size());

		for (Task task : pendingTasks) {
			try {
				log.info("Procesando Task ID: {}", task.getId());
				SurveyPanelistParticipation participation = task.getSurveyPanelistParticipation();
				if (participation == null) {
					log.error("No se encontró SurveyPanelistParticipation para Task ID: {}", task.getId());
					task.setStatus(TaskStatus.ERROR);
					taskService.save(task);
					continue;
				}

				Integer responseId = participation.getResponseId();
				Survey survey = participation.getSurvey();

				if (responseId == null || survey == null) {
					log.error("ResponseId o Survey no encontrado para SurveyPanelistParticipation ID: {}",
							participation.getId());
					task.setStatus(TaskStatus.ERROR);
					taskService.save(task);
					continue;
				}

				String alchemerSurveyId = survey.getAlchemerSurveyId();
				if (alchemerSurveyId == null || alchemerSurveyId.isEmpty()) {
					log.error("AlchemerSurveyId no encontrado para Survey ID: {}", survey.getId());
					task.setStatus(TaskStatus.ERROR);
					taskService.save(task);
					continue;
				}

				// Construir la URL para obtener la respuesta de la encuesta
				// https://api.alchemer.com/v5/survey/SURVEYID/surveyresponse/RESPONSEID
				UriComponentsBuilder surveyResponseUrlBuilder = UriComponentsBuilder.fromHttpUrl(ALCHEMER_API_BASE_URL)
						.pathSegment("survey", alchemerSurveyId, "surveyresponse", String.valueOf(responseId))
						.queryParam("api_token", apiToken).queryParam("api_token_secret", apiTokenSecret);

				String surveyResponseUrl = surveyResponseUrlBuilder.toUriString();
				log.info("Obteniendo respuesta de Alchemer: {}", surveyResponseUrl);

				ResponseEntity<Map> responseEntity = restTemplate.getForEntity(surveyResponseUrl, Map.class);

				if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
					Map<String, Object> surveyResponse = responseEntity.getBody();
					log.debug("Respuesta JSON de Alchemer: {}", surveyResponse);

					if (Boolean.TRUE.equals(surveyResponse.get("result_ok"))) {
						Map<String, Object> data = (Map<String, Object>) surveyResponse.get("data");
						if (data != null && data.containsKey("survey_data")) {
							Map<String, Object> surveyData = (Map<String, Object>) data.get("survey_data");
							if (surveyData != null) {
								for (Map.Entry<String, Object> entry : surveyData.entrySet()) {
									Map<String, Object> questionDetails = (Map<String, Object>) entry.getValue();
									if (questionDetails != null) {
										String questionText = String.valueOf(questionDetails.get("question"));
										String answerValue = String.valueOf(questionDetails.get("answer"));
										// El ID de la pregunta en este contexto es la clave de la entrada en surveyData
										String questionIdStr = entry.getKey();
										// El varname no está directamente en este nivel, se necesitaría si se quiere
										// mantener questionCode
										// Por ahora, podemos usar el ID como questionCode o dejarlo vacío si no es
										// crucial.
										// Para este refactor, asumiremos que questionCode puede ser el ID de la
										// pregunta si varname no está disponible.
										String questionCode = questionIdStr; // O buscar una alternativa si es
																				// necesario.

										if (questionText != null && !questionText.isEmpty() && answerValue != null) { // Allow
																														// empty
																														// answerValue
																														// for
																														// updates
											log.info("Procesando pregunta: ID={}, Pregunta='{}', Respuesta='{}'",
													questionIdStr, questionText, answerValue);

											// Check if an answer already exists
											Optional<AlchemerAnswer> existingAnswerOpt = answerService
													.findBySurveyPanelistParticipationAndQuestionCode(participation,
															questionCode);

											AlchemerAnswer answer;
											if (existingAnswerOpt.isPresent()) {
												// Update existing answer
												answer = existingAnswerOpt.get();
												if (!answer.getAnswer().equals(answerValue)) {
													answer.setAnswer(answerValue);
													answerService.save(answer);
													log.info(
															"Entidad Answer actualizada para questionCode: {}, participationId: {}",
															questionCode, participation.getId());
												} else {
													log.info(
															"Entidad Answer sin cambios para questionCode: {}, participationId: {}",
															questionCode, participation.getId());
												}
											} else {
												// Create new answer
												answer = new AlchemerAnswer();
												answer.setQuestion(questionText);

												answer.setAnswer(answerValue);
												answer.setSurveyPanelistParticipation(participation);
												answerService.save(answer);
												log.info(
														"Nueva entidad Answer guardada para questionCode: {}, participationId: {}",
														questionCode, participation.getId());
											}
											List<SurveyPropertyMatching> propertyMatchings = surveyPropertyMatchingService
													.findBySurvey(survey);
											for (SurveyPropertyMatching spm : propertyMatchings) {
												PanelistPropertyValue ppv = panelistPropertyValueService
														.findByPanelistAndPanelistProperty(participation.getPanelist(),
																spm.getProperty())
														.orElseGet(() -> {
															PanelistPropertyValue newPpv = new PanelistPropertyValue();
															newPpv.setPanelist(participation.getPanelist());
															newPpv.setPanelistProperty(spm.getProperty());
															return newPpv;
														});
												ppv.setValue(answer.getAnswer());
												ppv.setUpdated(new java.util.Date());
												panelistPropertyValueService.save(ppv);
											}
										} else {
											log.warn(
													"No se pudo obtener texto de pregunta o el valor de la respuesta es nulo (pero puede ser vacío) para la pregunta ID: {} en Survey ID: {}",
													questionIdStr, alchemerSurveyId);
										}
									} else {
										log.warn(
												"Detalles de pregunta nulos para una entrada en survey_data. Survey ID: {}",
												alchemerSurveyId);
									}
								}
							} else {
								log.warn(
										"El objeto 'survey_data' es nulo dentro de 'data' en la respuesta de Alchemer para Task ID: {}",
										task.getId());
							}
						} else {
							log.warn(
									"El objeto 'data' es nulo o no contiene 'survey_data' en la respuesta de Alchemer para Task ID: {}",
									task.getId());
						}
					} else {
						log.error("La API de Alchemer devolvió 'result_ok: false' para Task ID: {}. Respuesta: {}",
								task.getId(), surveyResponse);
						task.setStatus(TaskStatus.ERROR);
						taskService.save(task);
					}
				} else {
					log.error("Error al obtener la respuesta de Alchemer para Task ID: {}. Código de estado: {}",
							task.getId(), responseEntity.getStatusCode());
					task.setStatus(TaskStatus.ERROR);
					taskService.save(task);
				}
				// Si todo el procesamiento de respuestas para esta tarea fue exitoso (sin
				// entrar en los catch anteriores que setean ERROR)
				// marcamos la tarea como DONE.
				if (task.getStatus() == TaskStatus.PENDING) { // Solo si no se marcó como ERROR previamente
					task.setStatus(TaskStatus.DONE);
					taskService.save(task);
					log.info("Task ID: {} marcada como DONE.", task.getId());
				}
			} catch (HttpClientErrorException e) {
				log.error("Error (HttpClientErrorException) procesando Task ID: {}. Status: {}, Body: {}, Error: {}",
						task.getId(), e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage(), e);
				task.setStatus(TaskStatus.ERROR);
				taskService.save(task);
			} catch (RestClientException e) {
				log.error("Error (RestClientException) procesando Task ID: {}. Error: {}", task.getId(), e.getMessage(),
						e);
				task.setStatus(TaskStatus.ERROR);
				taskService.save(task);
			} catch (Exception e) { // Catch genérico para cualquier otra excepción no esperada durante el
									// procesamiento de la tarea
				log.error("Error inesperado procesando Task ID: {}. Error: {}", task.getId(), e.getMessage(), e);
				task.setStatus(TaskStatus.ERROR);
				taskService.save(task);
			}
		}

		log.info("Finalizada tarea AlchemerAnswerRetriever. Tareas procesadas: {}", pendingTasks.size());
	}

}
