package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation; // Added import

import java.util.List;
import java.util.Optional; // Added import

public interface AnswerRepository extends JpaRepository<Answer, Long>, JpaSpecificationExecutor<Answer> {

    Optional<Answer> findBySurveyPanelistParticipationAndQuestionCode(SurveyPanelistParticipation surveyPanelistParticipation, String questionCode);

    @EntityGraph(attributePaths = { "surveyPanelistParticipation.panelist" })
    List<Answer> findAll();
}
