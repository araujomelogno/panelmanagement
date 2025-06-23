package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Panel extends AbstractEntity {

    private String name;
    private LocalDate created;
    private boolean active;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public LocalDate getCreated() {
        return created;
    }
    public void setCreated(LocalDate created) {
        this.created = created;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    @ManyToMany(mappedBy = "panels", fetch = FetchType.LAZY)
    private Set<Panelist> panelists = new HashSet<>();

    public Set<Panelist> getPanelists() {
        return panelists;
    }

    public void setPanelists(Set<Panelist> panelists) {
        this.panelists = panelists;
    }
}
