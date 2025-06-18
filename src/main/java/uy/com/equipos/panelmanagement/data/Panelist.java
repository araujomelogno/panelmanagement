package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.Email;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
    public String getOccupation() {
        return occupation;
    }
    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }
    public LocalDate getLastContacted() {
        return lastContacted;
    }
    public void setLastContacted(LocalDate lastContacted) {
        this.lastContacted = lastContacted;
    }
    public LocalDate getLastInterviewed() {
        return lastInterviewed;
    }
    public void setLastInterviewed(LocalDate lastInterviewed) {
        this.lastInterviewed = lastInterviewed;
    }

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
        name = "panelist_panelist_property",
        joinColumns = @JoinColumn(name = "panelist_id"),
        inverseJoinColumns = @JoinColumn(name = "panelist_property_id")
    )
    private Set<PanelistProperty> properties = new HashSet<>();

    public Set<PanelistProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<PanelistProperty> properties) {
        this.properties = properties;
    }
}
