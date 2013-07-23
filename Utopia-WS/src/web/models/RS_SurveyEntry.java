package web.models;

import database.models.SurveyEntry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static com.google.common.base.Objects.firstNonNull;

@XmlType(name = "SurveyEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_SurveyEntry {
    @XmlElement(required = true, name = "BuildingType")
    private RS_Building building;

    @XmlElement(required = true, name = "EntryType")
    private String entryType;

    @XmlElement(required = true, name = "Value")
    private Integer value;

    public RS_SurveyEntry() {
    }

    RS_SurveyEntry(final SurveyEntry entry) {
        this.building = RS_Building.fromBuilding(entry.getBuilding());
        this.entryType = entry.getType().getName();
        this.value = entry.getValue();
    }

    public RS_Building getBuilding() {
        return building;
    }

    public String getEntryType() {
        return entryType;
    }

    public int getValue() {
        return firstNonNull(value, 0);
    }
}
