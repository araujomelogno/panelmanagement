package uy.com.equipos.panelmanagement.scheduler;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import uy.com.equipos.panelmanagement.data.AlchemerAnswer;
import uy.com.equipos.panelmanagement.data.JobType;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
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

@ExtendWith(MockitoExtension.class)
public class AlchemerAnswerRetrieverBorrarTest {

    @Mock
    private TaskService taskService;
    @Mock
    private SurveyPanelistParticipationService surveyPanelistParticipationService;
    @Mock
    private AnswerService answerService;
    @Mock
    private SurveyPropertyMatchingService surveyPropertyMatchingService;
    @Mock
    private PanelistPropertyValueService panelistPropertyValueService;
    @Mock
    private RestTemplate restTemplate;

    private AlchemerAnswerRetrieverBorrar alchemerAnswerRetriever;

    @BeforeEach
    void setUp() {
        alchemerAnswerRetriever = new AlchemerAnswerRetrieverBorrar(taskService, surveyPanelistParticipationService, answerService, surveyPropertyMatchingService, panelistPropertyValueService);
        org.springframework.test.util.ReflectionTestUtils.setField(alchemerAnswerRetriever, "restTemplate", restTemplate);
    }

    @Test
    void whenRetrieveAnswers_thenProcessAndCreatePanelistPropertyValue() {
        // Arrange
        Task task = new Task();
        task.setId(1L);
        task.setJobType(JobType.ALCHEMER_ANSWER_RETRIEVAL);
        task.setStatus(TaskStatus.PENDING);

        SurveyPanelistParticipation participation = new SurveyPanelistParticipation();
        participation.setId(1L);
        participation.setResponseId(12345);

        Survey survey = new Survey();
        survey.setId(1L);
        survey.setAlchemerSurveyId("67890");
        participation.setSurvey(survey);

        Panelist panelist = new Panelist();
        panelist.setId(1L);
        participation.setPanelist(panelist);

        task.setSurveyPanelistParticipation(participation);

        when(taskService.findAllByJobTypeAndStatus(JobType.ALCHEMER_ANSWER_RETRIEVAL, TaskStatus.PENDING))
            .thenReturn(Collections.singletonList(task));

        Map<String, Object> surveyData = new HashMap<>();
        Map<String, Object> questionDetails = new HashMap<>();
        questionDetails.put("question", "What is your favorite color?");
        questionDetails.put("answer", "Blue");
        surveyData.put("1", questionDetails);

        Map<String, Object> data = new HashMap<>();
        data.put("survey_data", surveyData);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("result_ok", true);
        responseBody.put("data", data);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        SurveyPropertyMatching spm = new SurveyPropertyMatching();
        spm.setSurvey(survey);
        PanelistProperty pp = new PanelistProperty();
        pp.setId(1L);
        spm.setProperty(pp);
        when(surveyPropertyMatchingService.findBySurvey(survey)).thenReturn(List.of(spm));

        when(answerService.findBySurveyPanelistParticipationAndQuestionCode(any(), anyString())).thenReturn(Optional.empty());
        when(panelistPropertyValueService.findByPanelistAndPanelistProperty(any(), any())).thenReturn(Optional.empty());

        // Act
        alchemerAnswerRetriever.retrieveAnswers();

        // Assert
        verify(taskService).save(task);
        assertEquals(TaskStatus.DONE, task.getStatus());

        verify(answerService).save(any(AlchemerAnswer.class));

        verify(panelistPropertyValueService).save(any(PanelistPropertyValue.class));
    }
}
