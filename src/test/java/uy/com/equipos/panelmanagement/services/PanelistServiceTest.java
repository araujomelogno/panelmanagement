package uy.com.equipos.panelmanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uy.com.equipos.panelmanagement.data.Panelist;
import uy.com.equipos.panelmanagement.data.PanelistProperty;
import uy.com.equipos.panelmanagement.data.PanelistRepository;
import uy.com.equipos.panelmanagement.data.PropertyType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PanelistServiceTest {

    @Mock
    private PanelistRepository panelistRepository;

    @InjectMocks
    private PanelistService panelistService;

    private PanelistProperty textProperty;
    private PanelistProperty dateProperty;

    @BeforeEach
    void setUp() {
        textProperty = new PanelistProperty();
        textProperty.setId(1L);
        textProperty.setName("Location");
        textProperty.setType(PropertyType.TEXTO);

        dateProperty = new PanelistProperty();
        dateProperty.setId(2L);
        dateProperty.setName("BirthDate");
        dateProperty.setType(PropertyType.FECHA);
    }

    @Test
    void findPanelistsByCriteria_nullCriteria_callsRepositoryFindAll() {
        when(panelistRepository.findAll()).thenReturn(Collections.singletonList(new Panelist()));

        List<Panelist> result = panelistService.findPanelistsByCriteria(null);

        assertThat(result).hasSize(1);
        verify(panelistRepository).findAll();
        verify(panelistRepository, never()).findByCriteria(anyMap());
    }

    @Test
    void findPanelistsByCriteria_emptyCriteria_callsRepositoryFindAll() {
        when(panelistRepository.findAll()).thenReturn(Collections.singletonList(new Panelist()));

        List<Panelist> result = panelistService.findPanelistsByCriteria(new HashMap<>());

        assertThat(result).hasSize(1);
        verify(panelistRepository).findAll();
        verify(panelistRepository, never()).findByCriteria(anyMap());
    }

    @Test
    void findPanelistsByCriteria_criteriaWithNullValue_filtersOutNullValueAndCallsFindAllIfEmpty() {
        Map<PanelistProperty, Object> criteria = new HashMap<>();
        criteria.put(textProperty, null);

        when(panelistRepository.findAll()).thenReturn(Collections.emptyList());

        panelistService.findPanelistsByCriteria(criteria);

        verify(panelistRepository).findAll(); // Because criteria becomes empty
        verify(panelistRepository, never()).findByCriteria(anyMap());
    }

    @Test
    void findPanelistsByCriteria_criteriaWithBlankStringValue_filtersOutBlankStringAndCallsFindAllIfEmpty() {
        Map<PanelistProperty, Object> criteria = new HashMap<>();
        criteria.put(textProperty, "   "); // Blank string

        when(panelistRepository.findAll()).thenReturn(Collections.emptyList());

        panelistService.findPanelistsByCriteria(criteria);

        verify(panelistRepository).findAll(); // Because criteria becomes empty
        verify(panelistRepository, never()).findByCriteria(anyMap());
    }

    @Test
    void findPanelistsByCriteria_validCriteria_callsRepositoryFindByCriteria() {
        Map<PanelistProperty, Object> criteria = new HashMap<>();
        criteria.put(textProperty, "Montevideo");

        when(panelistRepository.findByCriteria(anyMap())).thenReturn(Collections.singletonList(new Panelist()));

        List<Panelist> result = panelistService.findPanelistsByCriteria(criteria);

        assertThat(result).hasSize(1);
        verify(panelistRepository, never()).findAll();
        verify(panelistRepository).findByCriteria(eq(criteria)); // eq() to check map content
    }

    @Test
    void findPanelistsByCriteria_mixedCriteria_filtersNullsAndBlanks_callsRepositoryFindByCriteria() {
        Map<PanelistProperty, Object> criteria = new HashMap<>();
        criteria.put(textProperty, "Montevideo");
        criteria.put(dateProperty, null); // Should be filtered out
        PanelistProperty numericProperty = new PanelistProperty();
        numericProperty.setType(PropertyType.NUMERO); // Assuming PropertyType.NUMERO exists
        numericProperty.setId(3L); // Give it an ID for equals/hashCode if map relies on it
        numericProperty.setName("Age");
        criteria.put(numericProperty, "  "); // Should be filtered out

        Map<PanelistProperty, Object> expectedFilteredCriteria = new HashMap<>();
        expectedFilteredCriteria.put(textProperty, "Montevideo");

        when(panelistRepository.findByCriteria(anyMap())).thenReturn(Collections.emptyList());

        panelistService.findPanelistsByCriteria(criteria);

        verify(panelistRepository, never()).findAll();
        // Verifying the map content after internal filtering by the service
        verify(panelistRepository).findByCriteria(eq(expectedFilteredCriteria));
    }
}
