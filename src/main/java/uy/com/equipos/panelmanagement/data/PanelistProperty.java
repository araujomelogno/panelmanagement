package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class PanelistProperty extends AbstractEntity {

    private String name;
    private String type;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    @ManyToMany(mappedBy = "properties")
    private Set<Panelist> panelists = new HashSet<>();

    public Set<Panelist> getPanelists() {
        return panelists;
    }

    public void setPanelists(Set<Panelist> panelists) {
        this.panelists = panelists;
    }
}
