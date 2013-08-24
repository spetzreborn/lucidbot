package web.models;

import database.models.BuildEntry;
import database.models.Building;
import tools.validation.ExistsInDB;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;

@XmlRootElement(name = "BuildEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_BuildEntry {
    /**
     * The building type. Only the id for the building is ever used, so you don't have send in a complete Building object each time. The id
     * must point to a building that exists in the database however.
     * <p/>
     * Not updatable, but still has to be specified so that the build entry can be identified when updating.
     */
    @NotNull(message = "The building may not be null", groups = {Add.class, Update.class})
    @ExistsInDB(entity = Building.class, message = "No such building", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Building")
    private RS_Building building;

    /**
     * The target % for the building, which should be a value between 0 and 100.
     * <p/>
     * Updatable and mandatory for both adds and updates.
     */
    @NotNull(message = "Percentage may not be null", groups = {Add.class, Update.class})
    @DecimalMin(value = "0", message = "Percentage may not be a negative number", groups = {Add.class, Update.class})
    @DecimalMax(value = "100", message = "Percentage may not be over 100", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Percentage")
    private BigDecimal percentage;

    public RS_BuildEntry() {
    }

    RS_BuildEntry(final RS_Building building, final double percentage) {
        this.building = building;
        this.percentage = BigDecimal.valueOf(percentage);
    }

    public static RS_BuildEntry fromBuildEntry(final BuildEntry entry) {
        return new RS_BuildEntry(RS_Building.fromBuilding(entry.getBuilding()), entry.getPercentage());
    }

    public RS_Building getBuilding() {
        return building;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}
