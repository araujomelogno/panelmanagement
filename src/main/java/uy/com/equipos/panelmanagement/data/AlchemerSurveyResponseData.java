package uy.com.equipos.panelmanagement.data;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alchemer_survey_response_data")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlchemerSurveyResponseData extends AbstractEntity {

    @JsonProperty("is_test")
    private boolean isTest;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("response_id")
    private int responseId;

    @JsonProperty("account_id")
    private int accountId;

    @JsonProperty("survey_id")
    private int surveyId;

    @JsonProperty("response_status")
    private String responseStatus;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contact_id", referencedColumnName = "id")
    private AlchemerContact contact;

    @OneToOne(mappedBy = "data")
    private AlchemerSurveyResponse surveyResponse;

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

    public int getResponseId() {
        return responseId;
    }

    public void setResponseId(int responseId) {
        this.responseId = responseId;
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

    public AlchemerContact getContact() {
        return contact;
    }

    @JsonIgnore
    public void setContact(AlchemerContact contact) {
        if (contact != null) {
            contact.setSurveyResponseData(this);
        }
        this.contact = contact;
    }

    @JsonProperty("contact")
    @SuppressWarnings("unchecked")
    public void setContactValue(Object contactJson) {
        if (contactJson instanceof Map) {
            final ObjectMapper mapper = new ObjectMapper();
            AlchemerContact contact = mapper.convertValue(contactJson, AlchemerContact.class);
            setContact(contact);
        } else if (contactJson instanceof List) {
            setContact(null);
        }
    }

    public AlchemerSurveyResponse getSurveyResponse() {
        return surveyResponse;
    }

    public void setSurveyResponse(AlchemerSurveyResponse surveyResponse) {
        this.surveyResponse = surveyResponse;
    }
}
