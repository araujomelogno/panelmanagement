package uy.com.equipos.panelmanagement.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FormResponsePayload {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("data")
    private Data data;

    // Getters and setters

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        @JsonProperty("responseId")
        private String responseId;

        @JsonProperty("submissionId")
        private String submissionId;

        @JsonProperty("respondentId")
        private String respondentId;

        @JsonProperty("formId")
        private String formId;

        @JsonProperty("formName")
        private String formName;

        @JsonProperty("createdAt")
        private String createdAt;

        @JsonProperty("fields")
        private List<Field> fields;

        // Getters and setters

        public String getResponseId() {
            return responseId;
        }

        public void setResponseId(String responseId) {
            this.responseId = responseId;
        }

        public String getSubmissionId() {
            return submissionId;
        }

        public void setSubmissionId(String submissionId) {
            this.submissionId = submissionId;
        }

        public String getRespondentId() {
            return respondentId;
        }

        public void setRespondentId(String respondentId) {
            this.respondentId = respondentId;
        }

        public String getFormId() {
            return formId;
        }

        public void setFormId(String formId) {
            this.formId = formId;
        }

        public String getFormName() {
            return formName;
        }

        public void setFormName(String formName) {
            this.formName = formName;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public List<Field> getFields() {
            return fields;
        }

        public void setFields(List<Field> fields) {
            this.fields = fields;
        }
    }

    public static class Field {

        @JsonProperty("key")
        private String key;

        @JsonProperty("label")
        private String label;

        @JsonProperty("type")
        private String type;

        @JsonProperty("value")
        private String value;

        // Getters and setters

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
