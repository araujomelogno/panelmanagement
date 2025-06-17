package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import java.time.LocalDate;

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

}
