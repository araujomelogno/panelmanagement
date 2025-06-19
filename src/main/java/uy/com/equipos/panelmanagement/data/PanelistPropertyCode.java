package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;

@Entity
public class PanelistPropertyCode extends AbstractEntity {

    @NotBlank
    private String code;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panelist_property_id")
    private PanelistProperty panelistProperty;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PanelistProperty getPanelistProperty() {
        return panelistProperty;
    }

    public void setPanelistProperty(PanelistProperty panelistProperty) {
        this.panelistProperty = panelistProperty;
    }
}
