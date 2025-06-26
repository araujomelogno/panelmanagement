package uy.com.equipos.panelmanagement.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import uy.com.agesic.apptramites.lineadebase.domain.Tool;
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

    public Page<Survey> list(Pageable pageable, String name, LocalDate initDate, String link, Tool tool) {
        Specification<Survey> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (initDate != null) {
                predicates.add(cb.equal(root.get("initDate"), initDate));
            }
            if (link != null && !link.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("link")), "%" + link.toLowerCase() + "%"));
            }
            if (tool != null) {
                predicates.add(cb.equal(root.get("tool"), tool));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
