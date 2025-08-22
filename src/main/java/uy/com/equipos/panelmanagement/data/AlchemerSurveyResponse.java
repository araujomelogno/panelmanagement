package uy.com.equipos.panelmanagement.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alchemer_survey_response")
public class AlchemerSurveyResponse extends AbstractEntity {

	@JsonProperty("webhook_name")
	private String webhookName;

	@OneToOne(cascade = CascadeType.ALL)
	private AlchemerSurveyResponseData data;

	public String getWebhookName() {
		return webhookName;
	}

	public void setWebhookName(String webhookName) {
		this.webhookName = webhookName;
	}

	public AlchemerSurveyResponseData getData() {
		return data;
	}

	public void setData(AlchemerSurveyResponseData data) {
		if (data != null) {
			data.setSurveyResponse(this);
		}
		this.data = data;
	}
}
