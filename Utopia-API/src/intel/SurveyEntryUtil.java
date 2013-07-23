package intel;

import database.models.Building;
import database.models.Survey;
import database.models.SurveyEntry;

import java.util.Iterator;

public class SurveyEntryUtil {

    private SurveyEntryUtil() {
    }

    public static SurveyEntry registerEntry(final Survey survey, final Building building, final SurveyEntry.SurveyEntryType entryType,
                                            final int value) {
        for (SurveyEntry entry : survey.getBuildings()) {
            if (entry.getBuilding().equals(building) && entry.getType() == entryType) {
                entry.setValue(value);
                return entry;
            }
        }
        SurveyEntry surveyEntry = new SurveyEntry(survey, building, entryType, value);
        survey.getBuildings().add(surveyEntry);
        return surveyEntry;
    }

    public static void removeEntryWithBuilding(final Survey survey, final Building building) {
        for (Iterator<SurveyEntry> iter = survey.getBuildings().iterator(); iter.hasNext(); ) {
            SurveyEntry entry = iter.next();
            if (entry.getBuilding().equals(building)) iter.remove();
        }
    }
}
