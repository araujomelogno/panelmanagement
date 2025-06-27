package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
public class SurveyPanelistParticipation extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "survey_id")
    @NotNull
    private Survey survey;

    @ManyToOne
    @JoinColumn(name = "panelist_id")
    @NotNull
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
}
