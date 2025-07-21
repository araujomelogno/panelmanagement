package uy.com.equipos.panelmanagement.services;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPropertyMatching;
import uy.com.equipos.panelmanagement.data.SurveyPropertyMatchingRepository;

@Service
public class SurveyPropertyMatchingService {

    private final SurveyPropertyMatchingRepository repository;

    public SurveyPropertyMatchingService(SurveyPropertyMatchingRepository repository) {
        this.repository = repository;
    }

    public Optional<SurveyPropertyMatching> get(Long id) {
        return repository.findById(id);
    }

    public SurveyPropertyMatching save(SurveyPropertyMatching entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<SurveyPropertyMatching> findBySurvey(Survey survey) {
        return repository.findBySurvey(survey);
    }

    public Optional<SurveyPropertyMatching> findBySurveyAndProperty(Survey survey, PanelistProperty property) {
        return repository.findBySurveyAndProperty(survey, property);
    }
}
