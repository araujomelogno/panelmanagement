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

    // The relationship from Survey to MessageTask is now indirect via SurveyPanelistParticipation.
    // If you need to access MessageTasks related to a Survey, you would typically go through its participations.
    // For example, survey.getParticipations().stream().flatMap(p -> p.getMessageTasks().stream()).collect(Collectors.toSet());
    // Therefore, the direct @OneToMany MessageTask collection here might be removed or re-evaluated.
    // For now, we'll comment it out as it's no longer directly mapped by "survey" in MessageTask.
    // @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    // private Set<MessageTask> messageTasks = new HashSet<>();

    // public Set<MessageTask> getMessageTasks() {
    //     return messageTasks;
    // }

    // public void setMessageTasks(Set<MessageTask> messageTasks) {
    //     this.messageTasks = messageTasks;
    // }
}
