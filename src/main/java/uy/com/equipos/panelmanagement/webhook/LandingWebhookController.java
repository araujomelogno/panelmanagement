package uy.com.equipos.panelmanagement.webhook;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistRepository;
import uy.com.equipos.panelmanagement.data.Status;
import uy.com.equipos.panelmanagement.data.Task;
import uy.com.equipos.panelmanagement.data.TaskRepository;
import uy.com.equipos.panelmanagement.data.TaskStatus;
import uy.com.equipos.panelmanagement.webhook.dto.FormResponsePayload;

@RestController
@RequestMapping("/api/webhook")
public class LandingWebhookController {

	  private static final Logger LOG = LoggerFactory.getLogger(LandingWebhookController.class);

	    private final PanelistRepository panelistRepository;
	    private final TaskRepository taskRepository;

	    public LandingWebhookController(PanelistRepository panelistRepository, TaskRepository taskRepository) {
	        this.panelistRepository = panelistRepository;
	        this.taskRepository = taskRepository;
	    }

	    @PostMapping("/landing-response")
	    public ResponseEntity<Void> handleFormResponse(@RequestBody FormResponsePayload payload) {
	        LOG.info("Received form response: {}", payload.getEventId());

	        Map<String, String> fields = payload.getData().getFields().stream()
	            .collect(Collectors.toMap(FormResponsePayload.Field::getLabel, FormResponsePayload.Field::getValue));

	        String email = fields.get("Correo electrónico");
	        if (email == null || email.isEmpty()) {
	            LOG.warn("Received form response with empty email.");
	            return ResponseEntity.badRequest().build();
	        }

	        Optional<Panelist> existingPanelist = panelistRepository.findByEmail(email);

	        if (existingPanelist.isEmpty()) {
	            LOG.info("Creating new panelist with email: {}", email);
	            Panelist newPanelist = new Panelist();
	            newPanelist.setFirstName(fields.get("Nombre"));
	            newPanelist.setLastName(fields.get("Apellido"));
	            newPanelist.setEmail(email);
	            newPanelist.setStatus(Status.PENDIENTE);
	            panelistRepository.save(newPanelist);
	            LOG.info("New panelist created with ID: {}", newPanelist.getId());

	            Task task = new Task();
	            task.setJobType(JobType.SCREENING);
	            task.setStatus(TaskStatus.PENDING);
	            task.setCreated(LocalDateTime.now());
	            task.setPanelist(newPanelist);
	            taskRepository.save(task);
	            LOG.info("New screening task created for panelist ID: {}", newPanelist.getId());

	            return ResponseEntity.status(HttpStatus.CREATED).build();
	        } else {
	            LOG.info("Panelist with email {} already exists.", email);
	            return ResponseEntity.ok().build();
	        }
	    }
	}
