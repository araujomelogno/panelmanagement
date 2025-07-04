package uy.com.equipos.panelmanagement.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SurveyPanelistParticipationRepository
        extends
            JpaRepository<SurveyPanelistParticipation, Long>,
            JpaSpecificationExecutor<SurveyPanelistParticipation> {

    Optional<SurveyPanelistParticipation> findBySurveyAndPanelist(Survey survey, Panelist panelist);
}
