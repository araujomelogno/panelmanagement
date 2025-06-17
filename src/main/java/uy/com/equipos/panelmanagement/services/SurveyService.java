package uy.com.equipos.panelmanagement.services;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uy.com.equipos.panelmanagement.data.Survey;
import uy.com.equipos.panelmanagement.data.SurveyRepository;

@Service
public class SurveyService {

    private final SurveyRepository repository;

    public SurveyService(SurveyRepository repository) {
        this.repository = repository;
    }

    public Optional<Survey> get(Long id) {
        return repository.findById(id);
    }

    public Survey save(Survey entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Survey> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Survey> list(Pageable pageable, Specification<Survey> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
