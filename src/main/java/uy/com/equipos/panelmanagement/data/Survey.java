package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import uy.com.agesic.apptramites.lineadebase.domain.Tool;

@Entity
public class Survey extends AbstractEntity {

    private String name;
    private LocalDate initDate;
    private String link;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Tool tool;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "survey_panelist",
            joinColumns = @JoinColumn(name = "survey_id"),
            inverseJoinColumns = @JoinColumn(name = "panelist_id")
    )
    private Set<Panelist> panelists = new HashSet<>();

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

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public Set<Panelist> getPanelists() {
        return panelists;
    }

    public void setPanelists(Set<Panelist> panelists) {
        this.panelists = panelists;
    }
}
