package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "answers")
public class Answer extends AbstractEntity {

    private String question;
    private String questionCode;
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_panelist_participation_id")
    private SurveyPanelistParticipation surveyPanelistParticipation;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public SurveyPanelistParticipation getSurveyPanelistParticipation() {
        return surveyPanelistParticipation;
    }

    public void setSurveyPanelistParticipation(SurveyPanelistParticipation surveyPanelistParticipation) {
        this.surveyPanelistParticipation = surveyPanelistParticipation;
    }
}
