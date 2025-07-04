package uy.com.equipos.panelmanagement.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlchemerSurveyCompletionDataDto {

    @JsonProperty("is_test")
    private boolean isTest;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("account_id")
    private int accountId;

    @JsonProperty("survey_id")
    private int surveyId;

    @JsonProperty("response_status")
    private String responseStatus;

    @JsonProperty("url_variables")
    private AlchemerSurveyCompletionUrlVariablesDto urlVariables;

    @JsonProperty("survey_link")
    private AlchemerSurveyCompletionSurveyLinkDto surveyLink;

    private AlchemerSurveyCompletionContactDto contact;

    // Getters and Setters
    public boolean isTest() {
        return isTest;
    }

    public void setTest(boolean test) {
        isTest = test;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(int surveyId) {
        this.surveyId = surveyId;
    }

    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    public AlchemerSurveyCompletionUrlVariablesDto getUrlVariables() {
        return urlVariables;
    }

    public void setUrlVariables(AlchemerSurveyCompletionUrlVariablesDto urlVariables) {
        this.urlVariables = urlVariables;
    }

    public AlchemerSurveyCompletionSurveyLinkDto getSurveyLink() {
        return surveyLink;
    }

    public void setSurveyLink(AlchemerSurveyCompletionSurveyLinkDto surveyLink) {
        this.surveyLink = surveyLink;
    }

    public AlchemerSurveyCompletionContactDto getContact() {
        return contact;
    }

    public void setContact(AlchemerSurveyCompletionContactDto contact) {
        this.contact = contact;
    }
}
