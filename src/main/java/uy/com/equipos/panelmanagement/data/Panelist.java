package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
// import jakarta.persistence.JoinColumn; // Not strictly needed by Panelist's own fields now
// import jakarta.persistence.JoinTable; // Removed
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
// PanelistPropertyValue is in the same package

@Entity
public class Panelist extends AbstractEntity {

    private String firstName;
    private String lastName;
    @Email
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String occupation;
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
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
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
}
