package web.models;

import database.models.Building;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "SurveyEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_SurveyEntry {
    @XmlElement(required = true, name = "BuildingType")
    private RS_Building building;

    @XmlElement(required = true, name = "Built")
    private Integer built;

    @XmlElement(required = true, name = "InProgress")
    private Integer inProgress;

    public RS_SurveyEntry() {
    }

    RS_SurveyEntry(final Building building) {
        this.building = RS_Building.fromBuilding(building);
    }

    public RS_Building getBuilding() {
        return building;
    }

    public Integer getBuilt() {
        return built;
    }

    public void setBuilt(final Integer built) {
        this.built = built;
    }

    public Integer getInProgress() {
        return inProgress;
    }

    public void setInProgress(final Integer inProgress) {
        this.inProgress = inProgress;
    }
}
