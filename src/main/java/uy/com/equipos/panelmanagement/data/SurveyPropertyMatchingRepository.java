package uy.com.equipos.panelmanagement.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyPropertyMatchingRepository extends JpaRepository<SurveyPropertyMatching, Long> {
    Optional<SurveyPropertyMatching> findBySurveyAndProperty(Survey survey, PanelistProperty property);
    List<SurveyPropertyMatching> findBySurvey(Survey survey);
}
