package uy.com.equipos.panelmanagement.data;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

@Entity
public class Task extends AbstractEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @NotNull
    private LocalDateTime created;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    
    @ManyToOne
    @JoinColumn(name = "survey_panelist_participation_id")
    private SurveyPanelistParticipation surveyPanelistParticipation;

    @ManyToOne
    @JoinColumn(name = "survey_id")
    private Survey survey;

    @ManyToOne
    @JoinColumn(name = "panelist_id")
    private Panelist panelist;

    public Panelist getPanelist() {
        return panelist;
    }

    public void setPanelist(Panelist panelist) {
        this.panelist = panelist;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public SurveyPanelistParticipation getSurveyPanelistParticipation() {
        return surveyPanelistParticipation;
    }

    public void setSurveyPanelistParticipation(SurveyPanelistParticipation surveyPanelistParticipation) {
        this.surveyPanelistParticipation = surveyPanelistParticipation;
    }

    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }
}
