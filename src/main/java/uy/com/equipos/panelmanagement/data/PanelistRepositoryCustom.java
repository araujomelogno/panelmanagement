package uy.com.equipos.panelmanagement.data;

import java.util.List;
import java.util.Map;

public interface PanelistRepositoryCustom {
    List<Panelist> findByCriteria(Map<PanelistProperty, Object> criteria);
}
