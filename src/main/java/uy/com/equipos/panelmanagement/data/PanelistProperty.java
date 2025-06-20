package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class PanelistProperty extends AbstractEntity {

    private String name;

    @Enumerated(EnumType.STRING)
    private PropertyType type;

    @OneToMany(mappedBy = "panelistProperty", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PanelistPropertyCode> codes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PropertyType getType() {
        return type;
    }

    public void setType(PropertyType type) {
        this.type = type;
    }

    public List<PanelistPropertyCode> getCodes() {
        return codes;
    }

    public void setCodes(List<PanelistPropertyCode> codes) {
        this.codes = codes;
    }

    public void addCode(PanelistPropertyCode code) {
        codes.add(code);
        code.setPanelistProperty(this);
    }

    public void removeCode(PanelistPropertyCode code) {
        codes.remove(code);
        code.setPanelistProperty(null);
    }
}
