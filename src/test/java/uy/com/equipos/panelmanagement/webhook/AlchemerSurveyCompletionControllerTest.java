package uy.com.equipos.panelmanagement.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistRepository;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipationRepository;
import uy.com.equipos.panelmanagement.data.SurveyRepository;
import uy.com.equipos.panelmanagement.webhook.dto.AlchemerSurveyCompletionContactDto;
import uy.com.equipos.panelmanagement.webhook.dto.AlchemerSurveyCompletionDataDto;
import uy.com.equipos.panelmanagement.webhook.dto.AlchemerSurveyCompletionPayloadDto;
import uy.com.equipos.panelmanagement.webhook.dto.AlchemerSurveyCompletionSurveyLinkDto;
import uy.com.equipos.panelmanagement.webhook.dto.AlchemerSurveyCompletionUrlVariablesDto;


import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AlchemerSurveyCompletionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Para convertir DTO a JSON

    @MockBean
    private SurveyRepository surveyRepository;

    @MockBean
    private PanelistRepository panelistRepository;

    @MockBean
    private SurveyPanelistParticipationRepository surveyPanelistParticipationRepository;

    private AlchemerSurveyCompletionPayloadDto createValidPayload() {
        AlchemerSurveyCompletionPayloadDto payload = new AlchemerSurveyCompletionPayloadDto();
        payload.setWebhookName("On Response Received");

        AlchemerSurveyCompletionDataDto data = new AlchemerSurveyCompletionDataDto();
        data.setSurveyId(8378285); // Este ID se convertirá a String "8378285"
        data.setTest(false);
        data.setSessionId("test-session-id");
        data.setAccountId(12345);
        data.setResponseStatus("Complete");

        AlchemerSurveyCompletionContactDto contact = new AlchemerSurveyCompletionContactDto();
        contact.setEmail("test@example.com");
        contact.setFirstName("Test");
        contact.setLastName("User");
        data.setContact(contact);

        // UrlVariablesDto y SurveyLinkDto pueden ser nulos o tener valores por defecto
        // si no son cruciales para la lógica principal que estamos probando aquí.
        data.setUrlVariables(new AlchemerSurveyCompletionUrlVariablesDto());
        data.setSurveyLink(new AlchemerSurveyCompletionSurveyLinkDto());

        payload.setData(data);
        return payload;
    }

    @Test
    void handleSurveyResponse_success() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        String alchemerSurveyId = String.valueOf(payload.getData().getSurveyId());
        String email = payload.getData().getContact().getEmail();

        Survey survey = new Survey();
        survey.setId(1L);
        survey.setAlchemerSurveyId(alchemerSurveyId);

        Panelist panelist = new Panelist();
        panelist.setId(1L);
        panelist.setEmail(email);

        SurveyPanelistParticipation participation = new SurveyPanelistParticipation();
        participation.setId(1L);
        participation.setSurvey(survey);
        participation.setPanelist(panelist);
        participation.setCompleted(false);

        when(surveyRepository.findByAlchemerSurveyId(alchemerSurveyId)).thenReturn(Optional.of(survey));
        when(panelistRepository.findByEmail(email)).thenReturn(Optional.of(panelist));
        when(surveyPanelistParticipationRepository.findBySurveyAndPanelist(survey, panelist)).thenReturn(Optional.of(participation));
        when(surveyPanelistParticipationRepository.save(any(SurveyPanelistParticipation.class))).thenReturn(participation);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully. Participation updated."));

        verify(surveyPanelistParticipationRepository, times(1)).save(argThat(savedParticipation ->
            savedParticipation.isCompleted() && savedParticipation.getDateCompleted().equals(LocalDate.now())
        ));
    }

    @Test
    void handleSurveyResponse_surveyNotFound() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        String alchemerSurveyId = String.valueOf(payload.getData().getSurveyId());

        when(surveyRepository.findByAlchemerSurveyId(alchemerSurveyId)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Survey not found for alchemer_survey_id: " + alchemerSurveyId));

        verify(panelistRepository, never()).findByEmail(anyString());
        verify(surveyPanelistParticipationRepository, never()).findBySurveyAndPanelist(any(), any());
        verify(surveyPanelistParticipationRepository, never()).save(any());
    }

    @Test
    void handleSurveyResponse_panelistNotFound() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        String alchemerSurveyId = String.valueOf(payload.getData().getSurveyId());
        String email = payload.getData().getContact().getEmail();

        Survey survey = new Survey();
        survey.setId(1L);
        survey.setAlchemerSurveyId(alchemerSurveyId);

        when(surveyRepository.findByAlchemerSurveyId(alchemerSurveyId)).thenReturn(Optional.of(survey));
        when(panelistRepository.findByEmail(email)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Panelist not found for email: " + email));

        verify(surveyPanelistParticipationRepository, never()).findBySurveyAndPanelist(any(), any());
        verify(surveyPanelistParticipationRepository, never()).save(any());
    }

    @Test
    void handleSurveyResponse_participationNotFound() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        String alchemerSurveyId = String.valueOf(payload.getData().getSurveyId());
        String email = payload.getData().getContact().getEmail();

        Survey survey = new Survey();
        survey.setId(1L);
        survey.setAlchemerSurveyId(alchemerSurveyId);

        Panelist panelist = new Panelist();
        panelist.setId(1L);
        panelist.setEmail(email);

        when(surveyRepository.findByAlchemerSurveyId(alchemerSurveyId)).thenReturn(Optional.of(survey));
        when(panelistRepository.findByEmail(email)).thenReturn(Optional.of(panelist));
        when(surveyPanelistParticipationRepository.findBySurveyAndPanelist(survey, panelist)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Participation record not found for the given survey and panelist."));

        verify(surveyPanelistParticipationRepository, never()).save(any());
    }

    @Test
    void handleSurveyResponse_payloadMissingData() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        payload.setData(null); // Simulate missing data

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Incomplete payload: 'data' or 'contact' field is missing."));
    }

    @Test
    void handleSurveyResponse_payloadMissingContact() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        payload.getData().setContact(null); // Simulate missing contact

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Incomplete payload: 'data' or 'contact' field is missing."));
    }

    @Test
    void handleSurveyResponse_payloadMissingEmail() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        payload.getData().getContact().setEmail(null); // Simulate missing email

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email is missing in contact data."));
    }

    @Test
    void handleSurveyResponse_alreadyCompleted() throws Exception {
        AlchemerSurveyCompletionPayloadDto payload = createValidPayload();
        String alchemerSurveyId = String.valueOf(payload.getData().getSurveyId());
        String email = payload.getData().getContact().getEmail();

        Survey survey = new Survey();
        survey.setId(1L);
        survey.setAlchemerSurveyId(alchemerSurveyId);

        Panelist panelist = new Panelist();
        panelist.setId(1L);
        panelist.setEmail(email);

        SurveyPanelistParticipation participation = new SurveyPanelistParticipation();
        participation.setId(1L);
        participation.setSurvey(survey);
        participation.setPanelist(panelist);
        participation.setCompleted(true); // Already completed
        participation.setDateCompleted(LocalDate.now().minusDays(1)); // Completed yesterday

        when(surveyRepository.findByAlchemerSurveyId(alchemerSurveyId)).thenReturn(Optional.of(survey));
        when(panelistRepository.findByEmail(email)).thenReturn(Optional.of(panelist));
        when(surveyPanelistParticipationRepository.findBySurveyAndPanelist(survey, panelist)).thenReturn(Optional.of(participation));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/webhook/survey-response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Participation already marked as completed. No update performed."));

        verify(surveyPanelistParticipationRepository, never()).save(any());
    }
}
