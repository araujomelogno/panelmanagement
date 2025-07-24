package uy.com.equipos.panelmanagement.scheduler;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import uy.com.equipos.panelmanagement.services.PanelistService;
import uy.com.equipos.panelmanagement.services.ConfigurationItemService;

@Component
@EnableScheduling
public class RetricruitmentConfirmationSender {

    private static final Logger log = LoggerFactory.getLogger(RetricruitmentConfirmationSender.class);

    private final PanelistService panelistService;
    private final ConfigurationItemService configurationItemService;

    public RetricruitmentConfirmationSender(PanelistService panelistService,
                                          ConfigurationItemService configurationItemService) {
        this.panelistService = panelistService;
        this.configurationItemService = configurationItemService;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 */15 * * * *")
    public void sendConfirmation() {
        log.info("Iniciando tarea RetricruitmentConfirmationSender");

        Optional<uy.com.equipos.panelmanagement.data.ConfigurationItem> surveyLinkItem = configurationItemService.getByName("recruitment.alchemer.campaign.link");
        if (surveyLinkItem.isEmpty()) {
            log.error("No se encontró el item de configuración 'recruitment.alchemer.campaign.link'");
            return;
        }
        String surveyLink = surveyLinkItem.get().getValue();
        log.info("Survey Link: {}", surveyLink);

        List<uy.com.equipos.panelmanagement.data.Panelist> panelists = panelistService.findByStatusAndRecruitmentRetries(uy.com.equipos.panelmanagement.data.Status.PENDIENTE, 0);

        for (uy.com.equipos.panelmanagement.data.Panelist panelist : panelists) {
            log.info("Panelista: {} {} <{}>", panelist.getFirstName(), panelist.getLastName(), panelist.getEmail());
        }

        log.info("Finalizada tarea RetricruitmentConfirmationSender. Panelistas procesados: {}", panelists.size());
    }
}
