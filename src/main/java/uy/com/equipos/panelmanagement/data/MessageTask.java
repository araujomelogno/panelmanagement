package uy.com.equipos.panelmanagement.data;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

@Entity
public class MessageTask extends AbstractEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @NotNull
    private LocalDateTime created;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MessageTaskStatus status;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "survey_panelist_participation_id")
    private SurveyPanelistParticipation surveyPanelistParticipation;

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

    public MessageTaskStatus getStatus() {
        return status;
    }

    public void setStatus(MessageTaskStatus status) {
        this.status = status;
    }

    public SurveyPanelistParticipation getSurveyPanelistParticipation() {
        return surveyPanelistParticipation;
    }

    public void setSurveyPanelistParticipation(SurveyPanelistParticipation surveyPanelistParticipation) {
        this.surveyPanelistParticipation = surveyPanelistParticipation;
    }
}
