package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
// import jakarta.persistence.ManyToMany; // Removed
// import java.util.HashSet; // Removed
// import java.util.Set; // Removed

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

    // The panelists field and its getter/setter are removed from here.
}
