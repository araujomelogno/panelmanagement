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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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

    public AlchemerInviteSender(
        MessageTaskService messageTaskService,
        SurveyPanelistParticipationService surveyPanelistParticipationService,
        PanelistService panelistService
    ) {
        this.messageTaskService = messageTaskService;
        this.surveyPanelistParticipationService = surveyPanelistParticipationService;
        this.panelistService = panelistService;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void sendInvites() {
        log.info("Iniciando tarea AlchemerInviteSender");
        List<MessageTask> pendingTasks = messageTaskService.findAllByJobTypeAndStatus(
            JobType.ALCHEMER_INVITE,
            MessageTaskStatus.PENDING
        );

        for (MessageTask task : pendingTasks) {
            try {
                log.info("Procesando MessageTask ID: {}", task.getId());
                Optional<SurveyPanelistParticipation> participationOpt = surveyPanelistParticipationService.get(task.getSurveyPanelistParticipation().getId());

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
                    log.error("No se pudo obtener o crear el ContactID para el email: {} en la campaña del survey link: {}", email, surveyLink);
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

    private String getContactId(String surveyLinkOrCampaignId, String email) {
        String campaignId = extractCampaignId(surveyLinkOrCampaignId);
        String url = String.format("%s/v5/surveycampaign/%s/contact?email=%s&api_token=%s&api_token_secret=%s",
                                   ALCHEMER_API_BASE_URL, campaignId, email, apiToken, apiTokenSecret);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    return String.valueOf(data.get(0).get("id"));
                }
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                log.error("Error al verificar contacto {} en campaña {}: {}", email, campaignId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Excepción inesperada al verificar contacto {} en campaña {}: {}", email, campaignId, e.getMessage(), e);
        }
        return null;
    }

    private String addContactToCampaign(String surveyLinkOrCampaignId, String email, String firstName, String lastName) {
        String campaignId = extractCampaignId(surveyLinkOrCampaignId);
        String url = String.format("%s/v5/surveycampaign/%s/contact?api_token=%s&api_token_secret=%s",
                                   ALCHEMER_API_BASE_URL, campaignId, apiToken, apiTokenSecret);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("first_name", firstName);
        body.put("last_name", lastName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("id")) {
                    return String.valueOf(data.get("id"));
                }
            }
        } catch (Exception e) {
            log.error("Error al agregar contacto {} a campaña {}: {}", email, campaignId, e.getMessage(), e);
        }
        return null;
    }

    private boolean sendInvitation(String surveyLinkOrCampaignId, String contactId) {
        String campaignId = extractCampaignId(surveyLinkOrCampaignId);
        String url = String.format("%s/v5/surveycampaign/%s/send?api_token=%s&api_token_secret=%s",
                                   ALCHEMER_API_BASE_URL, campaignId, apiToken, apiTokenSecret);

        Map<String, List<String>> body = new HashMap<>();
        body.put("contact_ids", Collections.singletonList(contactId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, List<String>>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            // La API de Alchemer devuelve 200 OK incluso si hay problemas parciales,
            // pero para un solo contacto, un 200 debería ser suficiente.
            // Se podría mejorar parseando la respuesta para más detalles.
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Error al enviar invitación al contacto {} en campaña {}: {}", contactId, campaignId, e.getMessage(), e);
        }
        return false;
    }

    private String extractCampaignId(String surveyLink) {
        // Asume que el surveyLink es el ID de la campaña o contiene el ID.
        // Para la API de Alchemer, el {link obtenido en paso 3} se refiere al ID de la encuesta,
        // y en el contexto de /surveycampaign/{campaign_id}/..., campaign_id es el ID de la encuesta.
        // Si el 'link' es una URL completa, se necesitaría una lógica más robusta para extraer el ID.
        // Por ahora, asumimos que 'link' es directamente el campaign_id (survey_id).
        // Ejemplo: si surveyLink es "1234567", se usa "1234567".
        // Si fuera una URL como "https://app.alchemer.com/s3/1234567/My-Survey", se necesitaría extraer "1234567".
        // Esta implementación simple asume que el link es el ID.
        if (surveyLink != null && surveyLink.contains("/")) {
            // Intenta extraer el ID si es una URL simple, ej: /s3/1234567 o /s3/1234567/something
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
