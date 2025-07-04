package uy.com.equipos.panelmanagement.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookPayloadDto {

    @JsonProperty("webhook_name")
    private String webhookName;

    private WebhookDataDto data;

    // Getters and Setters
    public String getWebhookName() {
        return webhookName;
    }

    public void setWebhookName(String webhookName) {
        this.webhookName = webhookName;
    }

    public WebhookDataDto getData() {
        return data;
    }

    public void setData(WebhookDataDto data) {
        this.data = data;
    }
}
