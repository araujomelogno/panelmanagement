package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import java.time.LocalDate;

@Entity
public class Survey extends AbstractEntity {

    private String name;
    private LocalDate initDate;
    private String link;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public LocalDate getInitDate() {
        return initDate;
    }
    public void setInitDate(LocalDate initDate) {
        this.initDate = initDate;
    }
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }

}
