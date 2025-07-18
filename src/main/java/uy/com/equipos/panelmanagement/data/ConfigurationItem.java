package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;

@Entity
public class ConfigurationItem extends AbstractEntity {

    @NotBlank
    private String name;

    @NotBlank
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
