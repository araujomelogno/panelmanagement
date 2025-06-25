package uy.com.equipos.panelmanagement.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class PanelistRepositoryImpl implements PanelistRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Panelist> findByCriteria(Map<PanelistProperty, Object> criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Panelist> query = cb.createQuery(Panelist.class);
        Root<Panelist> panelistRoot = query.from(Panelist.class);

        if (criteria == null || criteria.isEmpty()) {
            query.select(panelistRoot);
            return entityManager.createQuery(query).getResultList();
        }

        List<Predicate> allCriteriaPredicates = new ArrayList<>();

        for (Map.Entry<PanelistProperty, Object> entry : criteria.entrySet()) {
            PanelistProperty property = entry.getKey();
            Object value = entry.getValue();

            if (value == null) continue;
            if (value instanceof String && ((String) value).isBlank()) continue;

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Panelist> subqueryPanelistRoot = subquery.correlate(panelistRoot);
            Join<Panelist, PanelistPropertyValue> ppvJoin = subqueryPanelistRoot.join("propertyValues");

            Predicate propertyMatch = cb.equal(ppvJoin.get("panelistProperty"), property);
            Predicate valueMatchPredicate = null;

            String valueAsString = null;

            PropertyType type = property.getType();
            if (type == null) {
                // Skip this criterion if property type is null
                continue;
            }

            switch (type) {
                case TEXTO:
                    if (value instanceof String) {
                        valueMatchPredicate = cb.like(cb.lower(ppvJoin.get("value")), "%" + ((String) value).toLowerCase() + "%");
                    }
                    break;
                case FECHA:
                    if (value instanceof LocalDate) {
                        valueAsString = ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
                        valueMatchPredicate = cb.equal(ppvJoin.get("value"), valueAsString);
                    }
                    break;
                case NUMERO:
                    if (value instanceof Number) {
                        valueAsString = value.toString();
                    } else if (value instanceof String) {
                        valueAsString = (String) value;
                    }
                    if (valueAsString != null && !valueAsString.isBlank()) { // Ensure not blank for numbers too
                        valueMatchPredicate = cb.equal(ppvJoin.get("value"), valueAsString);
                    }
                    break;
                case CODIGO:
                    if (value instanceof PanelistPropertyCode) {
                        valueAsString = ((PanelistPropertyCode) value).getCode();
                        valueMatchPredicate = cb.equal(ppvJoin.get("value"), valueAsString);
                    }
                    break;
            }

            if (valueMatchPredicate != null) {
                subquery.select(cb.literal(1L))
                        .where(cb.and(propertyMatch, valueMatchPredicate));
                allCriteriaPredicates.add(cb.exists(subquery));
            } else {
                // If a type is unhandled or value is of wrong type for a property,
                // that property effectively won't be part of the filter.
                // Or, we could add a predicate that is always false to ensure no results if a criterion is invalid.
                // For now, just skipping.
            }
        }

        if (!allCriteriaPredicates.isEmpty()) {
            query.where(cb.and(allCriteriaPredicates.toArray(new Predicate[0])));
        } else {
            // If all criteria were skipped (e.g. null/blank values, or type mismatches not leading to a predicate)
            // This means no effective filters were applied. Return all panelists or none based on requirements.
            // To return all if all criteria are skipped: (do nothing, as no query.where() is called)
            // To return none if all criteria are skipped: query.where(cb.disjunction()); // empty disjunction is false
            // Current behavior: returns all if allCriteriaPredicates is empty.
        }

        query.select(panelistRoot).distinct(true);
        return entityManager.createQuery(query).getResultList();
    }
}
