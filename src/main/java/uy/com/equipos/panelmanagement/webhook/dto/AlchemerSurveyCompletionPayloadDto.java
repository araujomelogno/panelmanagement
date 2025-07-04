package uy.com.equipos.panelmanagement.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlchemerSurveyCompletionPayloadDto {

    @JsonProperty("webhook_name")
    private String webhookName;

    private AlchemerSurveyCompletionDataDto data;

    // Getters and Setters
    public String getWebhookName() {
        return webhookName;
    }

    public void setWebhookName(String webhookName) {
        this.webhookName = webhookName;
    }

    public AlchemerSurveyCompletionDataDto getData() {
        return data;
    }

    public void setData(AlchemerSurveyCompletionDataDto data) {
        this.data = data;
    }
}
