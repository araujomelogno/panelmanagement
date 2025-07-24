package uy.com.equipos.panelmanagement.scheduler;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uy.com.equipos.panelmanagement.data.ConfigurationItem;
import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.Task;
import uy.com.equipos.panelmanagement.data.TaskStatus;
import uy.com.equipos.panelmanagement.services.ConfigurationItemService;
import uy.com.equipos.panelmanagement.services.TaskService;

@Component
@EnableScheduling
public class RetricruitmentConfirmationSender {

    private static final Logger log = LoggerFactory.getLogger(RetricruitmentConfirmationSender.class);

    private final TaskService taskService;
    private final ConfigurationItemService configurationItemService;

    public RetricruitmentConfirmationSender(TaskService taskService,
            ConfigurationItemService configurationItemService) {
        this.taskService = taskService;
        this.configurationItemService = configurationItemService;
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void sendConfirmation() {
        log.info("Iniciando tarea RetricruitmentConfirmationSender");
        List<Task> pendingTasks = taskService.findAllByJobTypeAndStatus(JobType.RECRUITMENT_CONFIRMATION,
                TaskStatus.PENDING);

        if (pendingTasks.isEmpty()) {
            log.info("No hay tareas pendientes de RECRUITMENT_CONFIRMATION");
            return;
        }

        Optional<ConfigurationItem> surveyLinkItem = configurationItemService
                .getByName("recruitment.alchemer.campaign.link");

        if (surveyLinkItem.isEmpty()) {
            log.error("No se encontró el item de configuración 'recruitment.alchemer.campaign.link'");
            return;
        }

        String surveyLink = surveyLinkItem.get().getValue();

        for (Task task : pendingTasks) {
            try {
                log.info("Procesando Task ID: {}", task.getId());

                Panelist panelist = task.getPanelist();
                if (panelist == null) {
                    log.error("Panelist no encontrado para Task ID: {}", task.getId());
                    task.setStatus(TaskStatus.ERROR);
                    taskService.save(task);
                    continue;
                }

                String email = panelist.getEmail();
                String firstName = panelist.getFirstName();
                String lastName = panelist.getLastName();

                log.info("Survey Link: {}", surveyLink);
                log.info("Panelist: {} {} <{}>", firstName, lastName, email);

                task.setStatus(TaskStatus.DONE);
                taskService.save(task);
                log.info("Task ID: {} marcada como DONE.", task.getId());

            } catch (Exception e) {
                log.error("Error procesando Task ID: {}. Error: {}", task.getId(), e.getMessage(), e);
                task.setStatus(TaskStatus.ERROR);
                taskService.save(task);
            }
        }
        log.info("Finalizada tarea RetricruitmentConfirmationSender. Tareas procesadas: {}", pendingTasks.size());
    }

}
