package uy.com.equipos.panelmanagement.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import uy.com.equipos.panelmanagement.data.Panel;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistRepository;
import uy.com.equipos.panelmanagement.data.Status;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import uy.com.equipos.panelmanagement.data.PanelistPropertyRepository;
import uy.com.equipos.panelmanagement.data.PanelistPropertyValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@Service
public class PanelistService {

    private final PanelistRepository repository;
    private final PanelistPropertyRepository panelistPropertyRepository;

    public PanelistService(PanelistRepository repository, PanelistPropertyRepository panelistPropertyRepository) {
        this.repository = repository;
        this.panelistPropertyRepository = panelistPropertyRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Panelist> get(Long id) {
        Optional<Panelist> panelistOptional = repository.findById(id);
        panelistOptional.ifPresent(panelist -> {
            Hibernate.initialize(panelist.getPropertyValues());
            Hibernate.initialize(panelist.getParticipations()); // Actualizado a participations
        });
        return panelistOptional;
    }

    // El método findByIdWithSurveys ya no es necesario o debe ser reemplazado
    // por una lógica que cargue las participaciones si se requiere.
    // Por ahora, lo comentamos o eliminamos.
    // @Transactional(readOnly = true)
    // public Optional<Panelist> findByIdWithParticipations(Long id) {
    //    return repository.findByIdWithParticipations(id);
    // }

    // Renamed from findByIdWithSurveys to better reflect its action due to get() initializing participations
    @Transactional(readOnly = true)
    public Optional<Panelist> findByIdWithParticipations(Long id) {
        // The existing get() method already initializes participations.
        return get(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    public Panelist save(Panelist entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Panelist> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Panelist> list(Pageable pageable, Specification<Panelist> filter) {
        return repository.findAll(filter, pageable);
    }

    public Page<Panelist> list(Pageable pageable, String firstName, String lastName, String email, String phone,
                               LocalDate lastContacted, LocalDate lastInterviewed, Status status) {
        Specification<Panelist> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (firstName != null && !firstName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
            }
            if (lastName != null && !lastName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (phone != null && !phone.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%"));
            }
            // Removed dateOfBirth filter logic
            // Removed occupation filter logic
            if (lastContacted != null) {
                predicates.add(cb.equal(root.get("lastContacted"), lastContacted));
            }
            if (lastInterviewed != null) {
                predicates.add(cb.equal(root.get("lastInterviewed"), lastInterviewed));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public Page<Panelist> listByStatus(Pageable pageable, String firstName, String lastName, String email, String phone, Status status) {
        Specification<Panelist> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (firstName != null && !firstName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
            }
            if (lastName != null && !lastName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
            }
            if (email != null && !email.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            }
            if (phone != null && !phone.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable);
    }

    public List<Panelist> findByStatus(Status status) {
        Specification<Panelist> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repository.findAll(spec);
    }

    public int count() {
        return (int) repository.count();
    }

    @Transactional(readOnly = true)
    public List<Panelist> findPanelistsByCriteria(Map<PanelistProperty, Object> filterCriteria) {
        if (filterCriteria == null || filterCriteria.isEmpty()) {
            return repository.findAll(); // Or an empty list, as per requirements
        }
        // Remove entries with null or blank string values before passing to repository
        Map<PanelistProperty, Object> finalCriteria = filterCriteria.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .filter(entry -> !(entry.getValue() instanceof String) || !((String) entry.getValue()).isBlank())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (finalCriteria.isEmpty()) {
             return repository.findAll(); // Or an empty list
        }
        return repository.findByCriteria(finalCriteria);
    }

    @Transactional
    public void removePanelFromPanelist(Long panelistId, Long panelId) {
        Optional<Panelist> panelistOpt = repository.findById(panelistId);
        if (panelistOpt.isPresent()) {
            Panelist panelist = panelistOpt.get();
            // Initialize panels collection if it's LAZY and not already fetched.
            // Since it's EAGER in Panelist.java, this explicit initialization might not be strictly necessary
            // but doesn't harm and ensures the collection is loaded if fetching strategy changes.
            Hibernate.initialize(panelist.getPanels());

            Optional<Panel> panelToRemoveOpt = panelist.getPanels().stream()
                    .filter(panel -> panel.getId().equals(panelId))
                    .findFirst();

            if (panelToRemoveOpt.isPresent()) {
                panelist.getPanels().remove(panelToRemoveOpt.get());
                // No explicit call to panelRepository is needed here for the Panel side
                // as Panelist is the owning side. Changes to panelist.getPanels()
                // will be persisted when the transaction commits after this method.
                // repository.save(panelist); // This save is implicit due to @Transactional
                                         // if the entity is managed and dirty.
                                         // However, to be absolutely explicit, especially if not relying
                                         // solely on dirty checking or if outside a transaction elsewhere,
                                         // an explicit save is safer. Given @Transactional, it should persist.
            } else {
                // Optionally, throw an exception or log if the panel is not associated
                // System.out.println("Panel with ID " + panelId + " not found in panelist's panels.");
                 throw new EntityNotFoundException("Panel with ID " + panelId + " not found in panelist's associated panels.");
            }
        } else {
            // Optionally, throw an exception or log if the panelist is not found
            // System.out.println("Panelist with ID " + panelistId + " not found.");
            throw new EntityNotFoundException("Panelist with ID " + panelistId + " not found.");
        }
    }

    public List<Panelist> findByStatusAndRecruitmentRetries(Status status, Integer recruitmentRetries) {
        return repository.findByStatusAndRecruitmentRetries(status, recruitmentRetries);
    }

    @Transactional
    public int importPanelistsFromData(Map<String, String> mappings, byte[] fileContent) throws IOException {
        List<Panelist> panelistsToSave = new ArrayList<>();
        try (InputStream inputStream = new ByteArrayInputStream(fileContent)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("El archivo Excel no tiene una fila de encabezado.");
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                headerMap.put(cell.getStringCellValue(), cell.getColumnIndex());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                Panelist panelist = new Panelist();

                for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                    Integer colIndex = headerMap.get(mapping.getKey());
                    if (colIndex == null) continue;

                    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String excelValue = (cell == null) ? "" : cell.toString().trim();

                    switch (mapping.getValue()) {
                        case "Nombre": panelist.setFirstName(excelValue); break;
                        case "Apellido": panelist.setLastName(excelValue); break;
                        case "E-mail": panelist.setEmail(excelValue); break;
                        case "Celular": panelist.setPhone(excelValue); break;
                        case "Fuente": panelist.setSource(excelValue); break;
                        case "Id_En_Fuente": panelist.setOriginalSourceId(excelValue); break;
                        default:
                            PanelistProperty prop = panelistPropertyRepository.findByName(mapping.getValue());
                            if (prop != null) {
                                PanelistPropertyValue propValue = new PanelistPropertyValue();
                                propValue.setPanelist(panelist);
                                propValue.setPanelistProperty(prop);
                                propValue.setValue(excelValue);
                                propValue.setUpdated(new java.util.Date());
                                panelist.getPropertyValues().add(propValue);
                            }
                            break;
                    }
                }
                panelistsToSave.add(panelist);
            }

            repository.saveAll(panelistsToSave);
            return panelistsToSave.size();
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        if (row.getLastCellNum() <= 0) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
