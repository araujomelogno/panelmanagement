package uy.com.equipos.panelmanagement.services;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipation;
import uy.com.equipos.panelmanagement.data.SurveyPanelistParticipationRepository;

@Service
public class SurveyPanelistParticipationService {

    private final SurveyPanelistParticipationRepository repository;

    public SurveyPanelistParticipationService(SurveyPanelistParticipationRepository repository) {
        this.repository = repository;
    }

    public Optional<SurveyPanelistParticipation> get(Long id) {
        return repository.findById(id);
    }

    public SurveyPanelistParticipation save(SurveyPanelistParticipation entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<SurveyPanelistParticipation> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<SurveyPanelistParticipation> list(Pageable pageable, Specification<SurveyPanelistParticipation> filter) {
        return repository.findAll(filter, pageable);
    }

    public List<SurveyPanelistParticipation> findBySurveyId(Long surveyId) {
        // Esto requeriría un método personalizado en el repositorio si no se usa Specification
        // Por ejemplo, en SurveyPanelistParticipationRepository:
        // List<SurveyPanelistParticipation> findBySurveyId(Long surveyId);
        // O usar Specification:
        return repository.findAll((root, query, cb) -> cb.equal(root.get("survey").get("id"), surveyId));
    }

    public boolean existsBySurveyAndPanelist(Survey survey, Panelist panelist) {
        return repository.findBySurveyAndPanelist(survey, panelist).isPresent();
    }

    public int count() {
        return (int) repository.count();
    }

}
