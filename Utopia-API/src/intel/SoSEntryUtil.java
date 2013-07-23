package intel;

import database.models.ScienceType;
import database.models.SoS;
import database.models.SoSEntry;

public class SoSEntryUtil {

    private SoSEntryUtil() {
    }

    public static SoSEntry registerEntry(final SoS sos,
                                         final SoSEntry.SoSEntryType entryType,
                                         final ScienceType scienceType,
                                         final double value) {
        for (SoSEntry entry : sos.getSciences()) {
            if (entry.getType() == entryType && entry.getScienceType().equals(scienceType)) {
                entry.setValue(value);
                return entry;
            }
        }
        SoSEntry soSEntry = new SoSEntry(sos, entryType, scienceType, value);
        sos.getSciences().add(soSEntry);
        return soSEntry;
    }
}
