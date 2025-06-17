package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;

@Entity
public class Request extends AbstractEntity {

    private String firstName;
    private String lastName;
    private String birhtdate;
    private String sex;
    @Email
    private String email;
    private String phone;

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
    public String getBirhtdate() {
        return birhtdate;
    }
    public void setBirhtdate(String birhtdate) {
        this.birhtdate = birhtdate;
    }
    public String getSex() {
        return sex;
    }
    public void setSex(String sex) {
        this.sex = sex;
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

}
