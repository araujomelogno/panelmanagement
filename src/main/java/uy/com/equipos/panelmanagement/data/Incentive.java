package uy.com.equipos.panelmanagement.data;

import jakarta.persistence.Entity;

@Entity
public class Incentive extends AbstractEntity {

    private String name;
    private Integer quantityAvailable;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getQuantityAvailable() {
        return quantityAvailable;
    }
    public void setQuantityAvailable(Integer quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

}
