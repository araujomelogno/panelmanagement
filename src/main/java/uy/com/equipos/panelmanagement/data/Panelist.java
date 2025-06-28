package uy.com.equipos.panelmanagement.data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors; // Added for stream operations

import uy.com.equipos.panelmanagement.data.Survey; // Added to reference Survey type

// PanelistPropertyValue is in the same package

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
// import jakarta.persistence.JoinColumn; // Not strictly needed by Panelist's own fields now
// import jakarta.persistence.JoinTable; // Removed
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;

@Entity
public class Panelist extends AbstractEntity {

    private String firstName;
    private String lastName;
    @Email
    private String email;
    private String phone;
    // private LocalDate dateOfBirth; // Removed
    // private String occupation; // Removed
    private LocalDate lastContacted;
    private LocalDate lastInterviewed;

    // Standard getters and setters for the above fields...
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    // public LocalDate getDateOfBirth() { return dateOfBirth; } // Removed
    // public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; } // Removed
    // public String getOccupation() { return occupation; } // Removed
    // public void setOccupation(String occupation) { this.occupation = occupation; } // Removed
    public LocalDate getLastContacted() { return lastContacted; }
    public void setLastContacted(LocalDate lastContacted) { this.lastContacted = lastContacted; }
    public LocalDate getLastInterviewed() { return lastInterviewed; }
    public void setLastInterviewed(LocalDate lastInterviewed) { this.lastInterviewed = lastInterviewed; }

    @OneToMany(
        mappedBy = "panelist",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private Set<PanelistPropertyValue> propertyValues = new HashSet<>();

    public Set<PanelistPropertyValue> getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(Set<PanelistPropertyValue> propertyValues) {
        this.propertyValues = propertyValues;
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "panelist_panel",
            joinColumns = @JoinColumn(name = "panelist_id"),
            inverseJoinColumns = @JoinColumn(name = "panel_id")
    )
    private Set<Panel> panels = new HashSet<>();

    public Set<Panel> getPanels() {
        return panels;
    }

    public void setPanels(Set<Panel> panels) {
        this.panels = panels;
    }

    @OneToMany(mappedBy = "panelist")
    private Set<SurveyPanelistParticipation> participations = new HashSet<>();

    public Set<SurveyPanelistParticipation> getParticipations() {
        return participations;
    }

    public void setParticipations(Set<SurveyPanelistParticipation> participations) {
        this.participations = participations;
    }

    /**
     * Gets the set of surveys this panelist has participated in.
     * This is derived from the 'participations' collection.
     * @return A Set of Survey objects. Returns an empty set if participations is null or empty.
     */
    public Set<Survey> getSurveys() {
        if (this.participations == null) {
            return new HashSet<>();
        }
        return this.participations.stream()
                                 .map(SurveyPanelistParticipation::getSurvey)
                                 .collect(Collectors.toSet());
    }

    // The relationship from Panelist to MessageTask is now indirect via SurveyPanelistParticipation.
    // Similar to the Survey entity, direct access to MessageTasks from Panelist might be re-evaluated.
    // Access would typically be: panelist.getParticipations().stream().flatMap(p -> p.getMessageTasks().stream()).collect(Collectors.toSet());
    // Commenting out for now.
    // @OneToMany(mappedBy = "panelist", cascade = CascadeType.ALL, orphanRemoval = true)
    // private Set<MessageTask> messageTasks = new HashSet<>();

    // public Set<MessageTask> getMessageTasks() {
    //     return messageTasks;
    // }

    // public void setMessageTasks(Set<MessageTask> messageTasks) {
    //     this.messageTasks = messageTasks;
    // }
}
