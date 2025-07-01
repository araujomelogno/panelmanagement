package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;


@Entity
public class SurveyPanelistParticipation extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "survey_id")
    @NotNull
    private Survey survey;

    @ManyToOne
    @JoinColumn(name = "panelist_id")
    private Panelist panelist;

    private LocalDate dateIncluded;
    private LocalDate dateSent;
    private boolean completed;

    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public Panelist getPanelist() {
        return panelist;
    }

    public void setPanelist(Panelist panelist) {
        this.panelist = panelist;
    }

    public LocalDate getDateIncluded() {
        return dateIncluded;
    }

    public void setDateIncluded(LocalDate dateIncluded) {
        this.dateIncluded = dateIncluded;
    }

    public LocalDate getDateSent() {
        return dateSent;
    }

    public void setDateSent(LocalDate dateSent) {
        this.dateSent = dateSent;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @OneToMany(mappedBy = "surveyPanelistParticipation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MessageTask> messageTasks = new HashSet<>();

    public Set<MessageTask> getMessageTasks() {
        return messageTasks;
    }

    public void setMessageTasks(Set<MessageTask> messageTasks) {
        this.messageTasks = messageTasks;
    }
}
