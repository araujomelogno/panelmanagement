package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.CascadeType;
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

    @OneToMany(mappedBy = "survey")
    private Set<SurveyPanelistParticipation> participations = new HashSet<>();

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

    public Set<SurveyPanelistParticipation> getParticipations() {
        return participations;
    }

    public void setParticipations(Set<SurveyPanelistParticipation> participations) {
        this.participations = participations;
    }

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MessageTask> messageTasks = new HashSet<>();

    public Set<MessageTask> getMessageTasks() {
        return messageTasks;
    }

    public void setMessageTasks(Set<MessageTask> messageTasks) {
        this.messageTasks = messageTasks;
    }
}
