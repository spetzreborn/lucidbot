package internal.templates;

import api.tools.time.TimeUtil;
import freemarker.template.SimpleDate;

import java.util.Date;

public class TemplateTimeUtil {

    /**
     * This is like {@link #compareDateToCurrent(java.util.Date)} except it's for freemarker SimpleDates instead
     *
     * @param date the date
     * @return a String detailing the hours, minutes and seconds between the specified date and the current time
     */
    public static String compareDateToCurrent(final SimpleDate date) {
        return TimeUtil.compareDateToCurrent(date.getAsDate());
    }

    /**
     * @param date the date
     * @return a String detailing the hours, minutes and seconds between the specified date and the current time
     */
    public static String compareDateToCurrent(final Date date) {
        return TimeUtil.compareTimeToCurrent(date.getTime());
    }

}
