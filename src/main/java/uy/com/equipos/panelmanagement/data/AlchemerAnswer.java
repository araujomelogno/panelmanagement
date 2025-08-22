package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alchemer_answer")
public class AlchemerAnswer extends AbstractEntity {

	@Id
	private Long id;
	private String type;
	@Column(columnDefinition = "MEDIUMTEXT")
	private String question;
	private Integer sectionId;
	@Column(columnDefinition = "MEDIUMTEXT")
	private String answer;
	private boolean shown;
	private Integer surveyId;
	private Integer responseId;
	private String questionCode;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "survey_panelist_participation_id")
	private SurveyPanelistParticipation surveyPanelistParticipation;

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getSectionId() {
		return sectionId;
	}

	public void setSectionId(Integer sectionId) {
		this.sectionId = sectionId;
	}

	public boolean isShown() {
		return shown;
	}

	public void setShown(boolean shown) {
		this.shown = shown;
	}

	public Integer getSurveyId() {
		return surveyId;
	}

	public void setSurveyId(Integer surveyId) {
		this.surveyId = surveyId;
	}

	public Integer getResponseId() {
		return responseId;
	}

	public void setResponseId(Integer responseId) {
		this.responseId = responseId;
	}

	public String getQuestionCode() {
		return questionCode;
	}

	public void setQuestionCode(String questionCode) {
		this.questionCode = questionCode;
	}

}
