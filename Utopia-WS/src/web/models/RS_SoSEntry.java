package web.models;

import database.models.SoSEntry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static com.google.common.base.Objects.firstNonNull;

@XmlType(name = "SoSEntry")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_SoSEntry {
    @XmlElement(required = true, name = "ScienceType")
    private RS_ScienceType scienceType;

    @XmlElement(required = true, name = "EntryType")
    private String entryType;

    @XmlElement(required = true, name = "Value")
    private Double value;

    public RS_SoSEntry() {
    }

    RS_SoSEntry(final SoSEntry entry) {
        this.scienceType = RS_ScienceType.fromScienceType(entry.getScienceType(), false);
        this.entryType = entry.getType().getName();
        this.value = entry.getValue();
    }

    public RS_ScienceType getScienceType() {
        return scienceType;
    }

    public String getEntryType() {
        return entryType;
    }

    public double getValue() {
        return firstNonNull(value, 0d);
    }
}
