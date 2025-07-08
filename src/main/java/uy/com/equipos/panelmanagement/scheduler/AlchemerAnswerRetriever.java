package uy.com.equipos.panelmanagement.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uy.com.equipos.panelmanagement.data.JobType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import uy.com.equipos.panelmanagement.data.*;
import uy.com.equipos.panelmanagement.services.AnswerService;
import uy.com.equipos.panelmanagement.services.SurveyPanelistParticipationService;
import uy.com.equipos.panelmanagement.services.TaskService;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@EnableScheduling
public class AlchemerAnswerRetriever {

    private static final Logger log = LoggerFactory.getLogger(AlchemerAnswerRetriever.class);

    private final TaskService taskService;
    private final SurveyPanelistParticipationService surveyPanelistParticipationService;
    private final AnswerService answerService;
    private final RestTemplate restTemplate;

    @Value("${alchemer.api.token}")
    private String apiToken;

    @Value("${alchemer.api.token.secret}")
    private String apiTokenSecret;

    private static final String ALCHEMER_API_BASE_URL = "https://api.alchemer.com/v5";

    public AlchemerAnswerRetriever(TaskService taskService,
                                   SurveyPanelistParticipationService surveyPanelistParticipationService,
                                   AnswerService answerService) {
        this.taskService = taskService;
        this.surveyPanelistParticipationService = surveyPanelistParticipationService;
        this.answerService = answerService;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(cron = "0 */5 * * * *") // Cada 5 minutos
    public void retrieveAnswers() {
        log.info("Iniciando tarea AlchemerAnswerRetriever");
        List<Task> pendingTasks = taskService.findAllByJobTypeAndStatus(JobType.ALCHEMER_ANSWER_RETRIEVAL, TaskStatus.PENDING);
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
                    log.error("ResponseId o Survey no encontrado para SurveyPanelistParticipation ID: {}", participation.getId());
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
                        .queryParam("api_token", apiToken)
                        .queryParam("api_token_secret", apiTokenSecret);

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
                                    // El varname no está directamente en este nivel, se necesitaría si se quiere mantener questionCode
                                    // Por ahora, podemos usar el ID como questionCode o dejarlo vacío si no es crucial.
                                    // Para este refactor, asumiremos que questionCode puede ser el ID de la pregunta si varname no está disponible.
                                    String questionCode = questionIdStr; // O buscar una alternativa si es necesario.

                                    if (questionText != null && !questionText.isEmpty() && answerValue != null && !answerValue.isEmpty()) {
                                        log.info("Pregunta encontrada: ID={}, Pregunta='{}', Respuesta='{}'", questionIdStr, questionText, answerValue);
                                        Answer answer = new Answer();
                                        answer.setQuestion(questionText);
                                        answer.setQuestionCode(questionCode); // Usar el ID de la pregunta como código
                                        answer.setAnswer(answerValue);
                                        answer.setSurveyPanelistParticipation(participation);
                                        answerService.save(answer);
                                        log.info("Entidad Answer guardada para questionId: {}, participationId: {}", questionIdStr, participation.getId());
                                    } else {
                                        log.warn("No se pudo obtener texto de pregunta o respuesta para la pregunta ID: {} en Survey ID: {}", questionIdStr, alchemerSurveyId);
                                        }
                                } else {
                                    log.warn("Detalles de pregunta nulos para una entrada en survey_data. Survey ID: {}", alchemerSurveyId);
                                    }
                                }
                        } else {
                            log.warn("El objeto 'survey_data' es nulo dentro de 'data' en la respuesta de Alchemer para Task ID: {}", task.getId());
                            }
                        } else {
                        log.warn("El objeto 'data' es nulo o no contiene 'survey_data' en la respuesta de Alchemer para Task ID: {}", task.getId());
                        }
                    } else {
                        log.error("La API de Alchemer devolvió 'result_ok: false' para Task ID: {}. Respuesta: {}", task.getId(), surveyResponse);
                        task.setStatus(TaskStatus.ERROR);
                        taskService.save(task);
                    }
                } else {
                    log.error("Error al obtener la respuesta de Alchemer para Task ID: {}. Código de estado: {}", task.getId(), responseEntity.getStatusCode());
                    task.setStatus(TaskStatus.ERROR);
                    taskService.save(task);
                }
                // Si todo el procesamiento de respuestas para esta tarea fue exitoso (sin entrar en los catch anteriores que setean ERROR)
                // marcamos la tarea como DONE.
                if (task.getStatus() == TaskStatus.PENDING) { // Solo si no se marcó como ERROR previamente
                    task.setStatus(TaskStatus.DONE);
                    taskService.save(task);
                    log.info("Task ID: {} marcada como DONE.", task.getId());
                }
            } catch (HttpClientErrorException e) {
                log.error("Error (HttpClientErrorException) procesando Task ID: {}. Status: {}, Body: {}, Error: {}", task.getId(), e.getStatusCode(), e.getResponseBodyAsString(), e.getMessage(), e);
                task.setStatus(TaskStatus.ERROR);
                taskService.save(task);
            } catch (RestClientException e) {
                log.error("Error (RestClientException) procesando Task ID: {}. Error: {}", task.getId(), e.getMessage(), e);
                task.setStatus(TaskStatus.ERROR);
                taskService.save(task);
            } catch (Exception e) { // Catch genérico para cualquier otra excepción no esperada durante el procesamiento de la tarea
                log.error("Error inesperado procesando Task ID: {}. Error: {}", task.getId(), e.getMessage(), e);
                task.setStatus(TaskStatus.ERROR);
                taskService.save(task);
            }
        }

        log.info("Finalizada tarea AlchemerAnswerRetriever. Tareas procesadas: {}", pendingTasks.size());
    }

    // private String extractQuestionId(String alchemerQuestionKey) {
    //     // Ejemplo: survey_data.question[question_id="123",SKU="10001"] -> 123
    //     // Ejemplo: question[123] -> 123
    //     // Ejemplo: question(123) -> 123
    //     // Ejemplo: [question(123)] -> 123
    //     // Ejemplo: [question_id=123] -> 123
    //     Pattern pattern = Pattern.compile("(?:question(?:_id)?(?:\\s*=\\s*|\\(|\\[))\"?(\\d+)\"?");
    //     Matcher matcher = pattern.matcher(alchemerQuestionKey);
    //     if (matcher.find()) {
    //         return matcher.group(1);
    //     }
    //     // Fallback para claves simples que podrían ser solo el ID numérico de la pregunta si la respuesta viene anidada de forma diferente
    //     // o si el formato es más directo como question_123_value
    //     Pattern simpleIdPattern = Pattern.compile("question_(\\d+)");
    //     Matcher simpleIdMatcher = simpleIdPattern.matcher(alchemerQuestionKey);
    //     if (simpleIdMatcher.find()){
    //         return simpleIdMatcher.group(1);
    //     }

    //     log.warn("No se pudo extraer el ID de la pregunta de la clave: {}", alchemerQuestionKey);
    //     return null;
    // }
}
