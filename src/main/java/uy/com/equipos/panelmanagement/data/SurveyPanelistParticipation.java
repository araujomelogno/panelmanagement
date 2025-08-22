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
    private LocalDate dateCompleted;
    private Integer responseId;

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

    public LocalDate getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(LocalDate dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    @OneToMany(mappedBy = "surveyPanelistParticipation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Task> messageTasks = new HashSet<>();

    @OneToMany(mappedBy = "surveyPanelistParticipation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AlchemerAnswer> answers = new HashSet<>();

    public Set<Task> getMessageTasks() {
        return messageTasks;
    }

    public void setMessageTasks(Set<Task> messageTasks) {
        this.messageTasks = messageTasks;
    }

    public Set<AlchemerAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(Set<AlchemerAnswer> answers) {
        this.answers = answers;
    }

	public Integer getResponseId() {
		return responseId;
	}

	public void setResponseId(Integer responseId) {
		this.responseId = responseId;
	}
}
