package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Entity
public class PanelistPropertyValue extends AbstractEntity {

    @NotNull 
    private String value;

    private Date updated;

    @ManyToOne(fetch = FetchType.LAZY) // LAZY es generalmente preferible para relaciones ManyToOne
    @JoinColumn(name = "panelist_id")
    @NotNull
    private Panelist panelist;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "panelist_property_id")
    @NotNull
    private PanelistProperty panelistProperty;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Panelist getPanelist() {
        return panelist;
    }

    public void setPanelist(Panelist panelist) {
        this.panelist = panelist;
    }

    public PanelistProperty getPanelistProperty() {
        return panelistProperty;
    }

    public void setPanelistProperty(PanelistProperty panelistProperty) {
        this.panelistProperty = panelistProperty;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
